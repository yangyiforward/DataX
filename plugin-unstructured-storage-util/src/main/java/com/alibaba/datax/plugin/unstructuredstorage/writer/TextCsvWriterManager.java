package com.alibaba.datax.plugin.unstructuredstorage.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvWriter;


public class TextCsvWriterManager {

    public static UnstructuredWriter produceTextWriter(Writer writer, String fieldDelimiter, Configuration config) {
        return new TextWriterImpl(writer, fieldDelimiter, config);
    }

    public static UnstructuredWriter produceDatWriter(Writer writer, char fieldDelimiter, Configuration config) {
        return new DatWriterImpl(writer, fieldDelimiter, config);
    }

    public static UnstructuredWriter produceCsvWriter(Writer writer, char fieldDelimiter, Configuration config) {
        return new CsvWriterImpl(writer, fieldDelimiter, config);
    }

    public static UnstructuredWriter produceSqlWriter(Writer writer, String quoteChar, String tableName, String lineSeparator, List<String> columnNames, String nullFormat) {
        return new SqlWriterImpl(writer, quoteChar, tableName, lineSeparator, columnNames, nullFormat);
    }
}

class CsvWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(CsvWriterImpl.class);
    // csv 严格符合csv语法, 有标准的转义等处理
    private char fieldDelimiter;
    private String lineDelimiter;
    private CsvWriter csvWriter;

    public CsvWriterImpl(Writer writer, char fieldDelimiter, Configuration config) {
        this.fieldDelimiter = fieldDelimiter;
        this.lineDelimiter = config.getString(Key.LINE_DELIMITER, IOUtils.LINE_SEPARATOR);
        this.csvWriter = new CsvWriter(writer, this.fieldDelimiter);
        this.csvWriter.setTextQualifier('"');
        this.csvWriter.setUseTextQualifier(true);
        // warn: in linux is \n , in windows is \r\n
        this.csvWriter.setRecordDelimiter(this.lineDelimiter.charAt(0));
    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
        }
        this.csvWriter.writeRecord(splitedRows.toArray(new String[0]));
    }

    @Override
    public void writeHeader(List<String> header) throws IOException {
        writeOneRecord(header);
    }

    @Override
    public void flush() throws IOException {
        this.csvWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.csvWriter.close();
    }

    @Override
    public void appendCommit() {
    }

}

class TextWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(TextWriterImpl.class);
    // text StringUtils的join方式, 简单的字符串拼接
    private String fieldDelimiter;
    private Writer textWriter;
    private String lineDelimiter;

    public TextWriterImpl(Writer writer, String fieldDelimiter, Configuration config) {
        this.fieldDelimiter = fieldDelimiter;
        this.textWriter = writer;
        this.lineDelimiter = config.getString(Key.LINE_DELIMITER, IOUtils.LINE_SEPARATOR);
    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
        }
        this.textWriter.write(String.format("%s%s",
                StringUtils.join(splitedRows, this.fieldDelimiter),
                this.lineDelimiter));
    }

    @Override
    public void writeHeader(List<String> header) throws IOException {
        writeOneRecord(header);
    }

    @Override
    public void flush() throws IOException {
        this.textWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.textWriter.close();
    }

    @Override
    public void appendCommit() {
    }

}

class DatWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(DatWriterImpl.class);
    // csv 严格符合csv语法, 有标准的转义等处理
    private char fieldDelimiter;
    private String lineDelimiter;
    private CsvWriter datWriter;

    public DatWriterImpl(Writer writer, char fieldDelimiter, Configuration config) {
        this.fieldDelimiter = fieldDelimiter;
        this.lineDelimiter = config.getString(Key.LINE_DELIMITER, IOUtils.LINE_SEPARATOR);
        this.datWriter = new CsvWriter(writer, this.fieldDelimiter);
        this.datWriter.setTextQualifier('"');
        this.datWriter.setUseTextQualifier(true);
        // warn: in linux is \n , in windows is \r\n
        this.datWriter.setRecordDelimiter(this.lineDelimiter.charAt(0));
    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
        }
        this.datWriter.writeRecord(splitedRows.toArray(new String[0]));
    }

    @Override
    public void writeHeader(List<String> header) throws IOException {
        if (header.isEmpty()) {
            LOG.warn("The datFile header is empty!");
        } else if (header.size() > 1) {
            LOG.warn("The datFile header is more than one, use the first element only!");
        }
        String headerStr = "<" + header.get(0) + ">";
        this.datWriter.writeRecord(new String[]{headerStr});
    }

    @Override
    public void flush() throws IOException {
        this.datWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.datWriter.close();
    }

    @Override
    public void appendCommit() {
    }

}

class SqlWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SqlWriterImpl.class);

    private Writer sqlWriter;
    private String quoteChar;
    private String lineSeparator;
    private String tableName;
    private String nullFormat;
    private StringBuilder insertPrefix;

    public SqlWriterImpl(Writer writer, String quoteChar, String tableName, String lineSeparator, List<String> columnNames, String nullFormat) {
        this.sqlWriter = writer;
        this.quoteChar = quoteChar;
        this.lineSeparator = lineSeparator;
        this.tableName = quoteChar + tableName + quoteChar;
        this.nullFormat = nullFormat;
        buildInsertPrefix(columnNames);
    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
            return;
        }

        StringBuilder sqlPatten = new StringBuilder(4096).append(insertPrefix);
        sqlPatten.append(splitedRows.stream().map(e -> {
            if (nullFormat.equals(e)) {
                return "NULL";
            }
            return "'" + replace(e, "'", "''") + "'";
        }).collect(Collectors.joining(",")));
        sqlPatten.append(");").append(lineSeparator);
        this.sqlWriter.write(sqlPatten.toString());
    }

    @Override
    public void writeHeader(List<String> header) throws IOException {
        writeOneRecord(header);
    }

    private void buildInsertPrefix(List<String> columnNames) {
        StringBuilder sb = new StringBuilder(columnNames.size() * 32);

        for (String columnName : columnNames) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(quoteChar).append(columnName).append(quoteChar);
        }

        int capacity = 16 + tableName.length() + sb.length();
        this.insertPrefix = new StringBuilder(capacity);
        this.insertPrefix.append("INSERT INTO ").append(tableName).append(" (").append(sb).append(")").append(" VALUES(");
    }

    private static String replace(String var0, String var1, String var2) {
        int var3 = var1.length();
        int var4 = var0.indexOf(var1);
        if(var4 <= -1) {
            return var0;
        } else {
            StringBuffer var5 = new StringBuffer();

            int var6;
            for(var6 = 0; var4 != -1; var4 = var0.indexOf(var1, var6)) {
                var5.append(var0.substring(var6, var4));
                var5.append(var2);
                var6 = var4 + var3;
            }

            var5.append(var0.substring(var6));
            return var5.toString();
        }
    }

    @Override
    public void appendCommit() throws IOException {
        this.sqlWriter.write("commit;" + lineSeparator);
    }

    @Override
    public void flush() throws IOException {
        this.sqlWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.sqlWriter.close();
    }
}
