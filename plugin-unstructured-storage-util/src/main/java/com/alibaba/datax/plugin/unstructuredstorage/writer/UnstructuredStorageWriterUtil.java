package com.alibaba.datax.plugin.unstructuredstorage.writer;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.alibaba.datax.common.element.BytesColumn;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.DateColumn;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.google.common.collect.Sets;

public class UnstructuredStorageWriterUtil {
    private UnstructuredStorageWriterUtil() {

    }

    private static final Logger LOG = LoggerFactory
            .getLogger(UnstructuredStorageWriterUtil.class);

    /**
     * check parameter: writeMode, encoding, compress, filedDelimiter
     * */
    public static void validateParameter(Configuration writerConfiguration) {
        // writeMode check
        String writeMode = writerConfiguration.getNecessaryValue(
                Key.WRITE_MODE,
                UnstructuredStorageWriterErrorCode.REQUIRED_VALUE);
        writeMode = writeMode.trim();
        Set<String> supportedWriteModes = Sets.newHashSet("truncate", "append",
                "nonConflict");
        if (!supportedWriteModes.contains(writeMode)) {
            throw DataXException
                    .asDataXException(
                            UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE,
                            String.format(
                                    "仅支持 truncate, append, nonConflict 三种模式, 不支持您配置的 writeMode 模式 : [%s]",
                                    writeMode));
        }
        writerConfiguration.set(Key.WRITE_MODE, writeMode);

        // encoding check
        String encoding = writerConfiguration.getString(Key.ENCODING);
        if (StringUtils.isBlank(encoding)) {
            // like "  ", null
            LOG.warn(String.format("您的encoding配置为空, 将使用默认值[%s]",
                    Constant.DEFAULT_ENCODING));
            writerConfiguration.set(Key.ENCODING, Constant.DEFAULT_ENCODING);
        } else {
            try {
                encoding = encoding.trim();
                writerConfiguration.set(Key.ENCODING, encoding);
                Charsets.toCharset(encoding);
            } catch (Exception e) {
                throw DataXException.asDataXException(
                        UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE,
                        String.format("不支持您配置的编码格式:[%s]", encoding), e);
            }
        }

        // only support compress types
        String compress = writerConfiguration.getString(Key.COMPRESS);
        if (StringUtils.isBlank(compress)) {
            writerConfiguration.set(Key.COMPRESS, null);
        } else {
            Set<String> supportedCompress = Sets.newHashSet("gzip", "bzip2");
            if (!supportedCompress.contains(compress.toLowerCase().trim())) {
                String message = String.format(
                        "仅支持 [%s] 文件压缩格式 , 不支持您配置的文件压缩格式: [%s]",
                        StringUtils.join(supportedCompress, ","), compress);
                throw DataXException.asDataXException(
                        UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE,
                        String.format(message, compress));
            }
        }

        // fileFormat check
        String fileFormat = writerConfiguration.getString(Key.FILE_FORMAT);
        if (StringUtils.isBlank(fileFormat)) {
            fileFormat = Constant.FILE_FORMAT_TEXT;
            writerConfiguration.set(Key.FILE_FORMAT, fileFormat);
        } else if (!Constant.FILE_FORMAT_CSV.equals(fileFormat)
                && !Constant.FILE_FORMAT_TEXT.equals(fileFormat)
                && !Constant.FILE_FORMAT_DAT.equals(fileFormat)
                && !Constant.FILE_FORMAT_SQL.equals(fileFormat)) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE, String
                            .format("您配置的fileFormat [%s]错误, 仅支持 csv, text, dat, sql 四种格式.",
                                    fileFormat));
        } else if (Constant.FILE_FORMAT_DAT.equals(fileFormat)) {
            List<String> column = writerConfiguration.getList(Key.COLUMN, String.class);
            if (CollectionUtils.isEmpty(column)) {
                LOG.warn(String.format("您配置的fileFormat为[%s], 配置的column为空.", fileFormat));
            }
        }

        // fieldDelimiter check
        String delimiterInStr = writerConfiguration.getString(Key.FIELD_DELIMITER);
        // warn: if have, length must be one
        if (null == delimiterInStr) {
            LOG.warn(String.format("您的delimiter配置为空, 将使用默认值[%s]",
                    Constant.DEFAULT_FIELD_DELIMITER));
            delimiterInStr = String.valueOf(Constant.DEFAULT_FIELD_DELIMITER);
            writerConfiguration.set(Key.FIELD_DELIMITER, delimiterInStr);
        } else if (Constant.FILE_FORMAT_CSV.equals(fileFormat) && 1 != delimiterInStr.length()) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE,
                    String.format("仅仅支持单字符切分, 您配置的切分为 : [%s]", delimiterInStr));
        }

    }

    public static List<Configuration> split(Configuration writerSliceConfig,
                                            Set<String> originAllFileExists, int mandatoryNumber) {
        LOG.info("begin do split...");
        Set<String> allFileExists = new HashSet<String>();
        allFileExists.addAll(originAllFileExists);
        List<Configuration> writerSplitConfigs = new ArrayList<Configuration>();
        String filePrefix = writerSliceConfig.getString(Key.FILE_NAME);

        String fileSuffix;
        for (int i = 0; i < mandatoryNumber; i++) {
            // handle same file name
            Configuration splitedTaskConfig = writerSliceConfig.clone();
            String fullFileName = null;
            fileSuffix = UUID.randomUUID().toString().replace('-', '_');
            fullFileName = String.format("%s__%s", filePrefix, fileSuffix);
            while (allFileExists.contains(fullFileName)) {
                fileSuffix = UUID.randomUUID().toString().replace('-', '_');
                fullFileName = String.format("%s__%s", filePrefix, fileSuffix);
            }
            allFileExists.add(fullFileName);
            splitedTaskConfig.set(Key.FILE_NAME, fullFileName);
            LOG.info(String
                    .format("splited write file name:[%s]", fullFileName));
            writerSplitConfigs.add(splitedTaskConfig);
        }
        LOG.info("end do split.");
        return writerSplitConfigs;
    }

    public static String buildFilePath(String path, String fileName,
                                       String suffix) {
        boolean isEndWithSeparator = false;
        switch (IOUtils.DIR_SEPARATOR) {
            case IOUtils.DIR_SEPARATOR_UNIX:
                isEndWithSeparator = path.endsWith(String
                        .valueOf(IOUtils.DIR_SEPARATOR));
                break;
            case IOUtils.DIR_SEPARATOR_WINDOWS:
                isEndWithSeparator = path.endsWith(String
                        .valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                break;
            default:
                break;
        }
        if (!isEndWithSeparator) {
            path = path + IOUtils.DIR_SEPARATOR;
        }
        if (null == suffix) {
            suffix = "";
        } else {
            suffix = suffix.trim();
        }
        return String.format("%s%s%s", path, fileName, suffix);
    }

    public static void writeToStream(RecordReceiver lineReceiver,
                                     OutputStream outputStream, Configuration config, String context,
                                     TaskPluginCollector taskPluginCollector) {
        String encoding = config.getString(Key.ENCODING,
                Constant.DEFAULT_ENCODING);
        // handle blank encoding
        if (StringUtils.isBlank(encoding)) {
            LOG.warn(String.format("您配置的encoding为[%s], 使用默认值[%s]", encoding,
                    Constant.DEFAULT_ENCODING));
            encoding = Constant.DEFAULT_ENCODING;
        }
        String compress = config.getString(Key.COMPRESS);

        BufferedWriter writer = null;
        // compress logic
        try {
            if (null == compress) {
                writer = new BufferedWriter(new OutputStreamWriter(
                        outputStream, encoding));
            } else {
                // TODO more compress
                if ("gzip".equalsIgnoreCase(compress)) {
                    CompressorOutputStream compressorOutputStream = new GzipCompressorOutputStream(
                            outputStream);
                    writer = new BufferedWriter(new OutputStreamWriter(
                            compressorOutputStream, encoding));
                } else if ("bzip2".equalsIgnoreCase(compress)) {
                    CompressorOutputStream compressorOutputStream = new BZip2CompressorOutputStream(
                            outputStream);
                    writer = new BufferedWriter(new OutputStreamWriter(
                            compressorOutputStream, encoding));
                } else {
                    throw DataXException
                            .asDataXException(
                                    UnstructuredStorageWriterErrorCode.ILLEGAL_VALUE,
                                    String.format(
                                            "仅支持 gzip, bzip2 文件压缩格式 , 不支持您配置的文件压缩格式: [%s]",
                                            compress));
                }
            }
            UnstructuredStorageWriterUtil.doWriteToStream(lineReceiver, writer,
                    context, config, taskPluginCollector);
        } catch (UnsupportedEncodingException uee) {
            throw DataXException
                    .asDataXException(
                            UnstructuredStorageWriterErrorCode.WRITE_FILE_WITH_CHARSET_ERROR,
                            String.format("不支持的编码格式 : [%s]", encoding), uee);
        } catch (NullPointerException e) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.RUNTIME_EXCEPTION,
                    "运行时错误", e);
        } catch (IOException e) {
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.WRITE_FILE_IO_ERROR,
                    String.format("流写入错误 : [%s]", context), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private static void doWriteToStream(RecordReceiver lineReceiver,
                                        BufferedWriter writer, String contex, Configuration config,
                                        TaskPluginCollector taskPluginCollector) throws IOException {

        String nullFormat = config.getString(Key.NULL_FORMAT);

        // 兼容format & dataFormat
        String dateFormat = config.getString(Key.DATE_FORMAT);
        DateFormat dateParse = null; // warn: 可能不兼容
        if (StringUtils.isNotBlank(dateFormat)) {
            dateParse = new SimpleDateFormat(dateFormat);
        }

        // warn: default false
        String fileFormat = config.getString(Key.FILE_FORMAT, Constant.FILE_FORMAT_TEXT);
        boolean isSqlFormat = Constant.FILE_FORMAT_SQL.equals(fileFormat);
        int commitSize = config.getInt(Key.COMMIT_SIZE, Constant.DEFAULT_COMMIT_SIZE);
        UnstructuredWriter unstructuredWriter = produceUnstructuredWriter(fileFormat, config, writer);

        String fieldDelimiter = config.getString(Key.FIELD_DELIMITER, String.valueOf(Constant.DEFAULT_FIELD_DELIMITER));

        List<String> headers = config.getList(Key.HEADER, String.class);
        if (null != headers && !headers.isEmpty() && !isSqlFormat) {
            unstructuredWriter.writeHeader(headers);
        }

        List<String> columnName = Constant.FILE_FORMAT_DAT.equals(fileFormat)
                ? config.getList(Key.COLUMN, String.class)
                : new ArrayList<>();

        Record record = null;
        int receivedCount = 0;
        String byteEncoding = config.getString(Key.BYTE_ENCODING);
        while ((record = lineReceiver.getFromReader()) != null) {
            UnstructuredStorageWriterUtil.transportOneRecord(record,
                    nullFormat, fieldDelimiter, dateParse, taskPluginCollector,
                    unstructuredWriter, columnName, byteEncoding);
            receivedCount++;
            if (isSqlFormat && receivedCount % commitSize == 0) {
                unstructuredWriter.appendCommit();
            }
        }

        if (isSqlFormat) {
            unstructuredWriter.appendCommit();
        }
        // warn:由调用方控制流的关闭
        // IOUtils.closeQuietly(unstructuredWriter);
    }

    public static UnstructuredWriter produceUnstructuredWriter(String fileFormat, Configuration config, Writer writer){
        UnstructuredWriter unstructuredWriter = null;
        if (StringUtils.equalsIgnoreCase(fileFormat, Constant.FILE_FORMAT_CSV)) {

            Character fieldDelimiter = config.getChar(Key.FIELD_DELIMITER, Constant.DEFAULT_FIELD_DELIMITER);
            unstructuredWriter = TextCsvWriterManager.produceCsvWriter(writer, fieldDelimiter, config);
        } else if (StringUtils.equalsIgnoreCase(fileFormat, Constant.FILE_FORMAT_TEXT)) {

            String fieldDelimiter = config.getString(Key.FIELD_DELIMITER, String.valueOf(Constant.DEFAULT_FIELD_DELIMITER));
            unstructuredWriter = TextCsvWriterManager.produceTextWriter(writer, fieldDelimiter, config);
        } else if (StringUtils.equalsIgnoreCase(fileFormat, Constant.FILE_FORMAT_DAT)) {

            Character fieldDelimiter = config.getChar(Key.FIELD_DELIMITER, Constant.DEFAULT_FIELD_DELIMITER);
            if (fieldDelimiter == '~') {
                LOG.info("The fieldDelimiter is '~', so the row will be break by columns");
            }
            unstructuredWriter = TextCsvWriterManager.produceDatWriter(writer, fieldDelimiter, config);
        } else if (StringUtils.equalsIgnoreCase(fileFormat, Constant.FILE_FORMAT_SQL)) {

            String tableName = config.getString(Key.TABLE_NAME);
            Preconditions.checkArgument(StringUtils.isNotEmpty(tableName), "table name is empty");
            String quoteChar = config.getString(Key.QUOTE_CHARACTER);
            Preconditions.checkArgument(StringUtils.isNotEmpty(quoteChar), "quote character is empty");
            String lineSeparator = config.getString(Key.LINE_DELIMITER, IOUtils.LINE_SEPARATOR);
            List<String> headers = config.getList(Key.HEADER, String.class);
            Preconditions.checkArgument(CollectionUtils.isNotEmpty(headers), "column names are empty");
            String nullFormat = config.getString(Key.NULL_FORMAT, Constant.DEFAULT_NULL_FORMAT);
            unstructuredWriter = TextCsvWriterManager.produceSqlWriter(writer, quoteChar, tableName, lineSeparator, headers, nullFormat);
        }

        return unstructuredWriter;
    }

    /**
     * 异常表示脏数据
     * */
    public static void transportOneRecord(Record record, String nullFormat, String fieldDelimiter,
                                          DateFormat dateParse, TaskPluginCollector taskPluginCollector,
                                          UnstructuredWriter unstructuredWriter, List<String> columnName, String byteEncoding) {
        // warn: default is null
        if (null == nullFormat) {
            nullFormat = "null";
        }

        int columnLength = 0;
        if (CollectionUtils.isNotEmpty(columnName)) {
            columnLength = columnName.size();
        }
        try {
            List<String> splitedRows = new ArrayList<String>();
            int recordLength = record.getColumnNumber();
            boolean columnReady = recordLength == columnLength;

            if (0 != recordLength) {
                Column column;
                for (int i = 0; i < recordLength; i++) {
                    column = record.getColumn(i);
                    if (null != column.getRawData()) {
                        boolean isDateColumn = column instanceof DateColumn;
                        if (!isDateColumn) {
                            if (column instanceof BytesColumn) {
                                if ("base64".equalsIgnoreCase(byteEncoding)) {
                                    if (columnReady && ("~".equals(fieldDelimiter) || i > 0)) {
                                        splitedRows.add(columnName.get(i) + "=" + Base64.encodeBase64String(column.asBytes()));
                                    } else {
                                        splitedRows.add(Base64.encodeBase64String(column.asBytes()));
                                    }
                                } else {
                                    if (columnReady && ("~".equals(fieldDelimiter) || i > 0)) {
                                        splitedRows.add(columnName.get(i) + "=" + column.asString());
                                    } else {
                                        splitedRows.add(column.asString());
                                    }
                                }
                            } else {
                                if (columnReady && ("~".equals(fieldDelimiter) || i > 0)) {
                                    splitedRows.add(columnName.get(i) + "=" + column.asString());
                                } else {
                                    splitedRows.add(column.asString());
                                }
                            }
                        } else {
                            if (null != dateParse) {
                                if (columnReady && ("~".equals(fieldDelimiter) || i > 0)) {
                                    splitedRows.add(columnName.get(i) + "=" + dateParse.format(column.asDate()));
                                } else {
                                    splitedRows.add(dateParse.format(column.asDate()));
                                }
                            } else {
                                if (columnReady && ("~".equals(fieldDelimiter) || i > 0)) {
                                    splitedRows.add(columnName.get(i) + "=" + column.asString());
                                } else {
                                    splitedRows.add(column.asString());
                                }
                            }
                        }
                    } else {
                        // warn: it's all ok if nullFormat is null
                        splitedRows.add(nullFormat);
                    }
                }
            }
            unstructuredWriter.writeOneRecord(splitedRows);
        } catch (IllegalArgumentException e){
            // warn: dirty data
            taskPluginCollector.collectDirtyRecord(record, e);
        } catch (DataXException e){
            // warn: dirty data
            taskPluginCollector.collectDirtyRecord(record, e);
        } catch (Exception e) {
            // throw exception, it is not dirty data,
            // may be network unreachable and the other problem
            throw DataXException.asDataXException(
                    UnstructuredStorageWriterErrorCode.WRITE_ERROR, e.getMessage(),e);
        }
    }

}
