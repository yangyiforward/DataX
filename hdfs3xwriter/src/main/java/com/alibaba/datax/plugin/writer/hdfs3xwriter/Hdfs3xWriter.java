package com.alibaba.datax.plugin.writer.hdfs3xwriter;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.google.common.collect.Sets;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class Hdfs3xWriter extends Writer {
    public static class Job extends Writer.Job {
        private static final Logger LOG = LoggerFactory.getLogger(Job.class);

        private Configuration writerSliceConfig = null;

        private String defaultFS;
        private String path;
        private String fileType;
        private String fileName;
        private List<Configuration> columns;
        private String writeMode;
        private String fieldDelimiter;
        private String compress;
        private String encoding;
        private HashSet<String> tmpFiles = new HashSet<String>();//临时文件全路径
        private HashSet<String> endFiles = new HashSet<String>();//最终文件全路径

        private HdfsHelper hdfsHelper = null;

        @Override
        public void init() {
            this.writerSliceConfig = this.getPluginJobConf();
            this.validateParameter();

            //创建textfile存储
            hdfsHelper = new HdfsHelper();

            hdfsHelper.getFileSystem(defaultFS, this.writerSliceConfig);
        }

        private void validateParameter() {
            this.defaultFS = this.writerSliceConfig.getNecessaryValue(Key.DEFAULT_FS, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
            //fileType check
            this.fileType = this.writerSliceConfig.getNecessaryValue(Key.FILE_TYPE, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
            if(!fileType.equalsIgnoreCase("ORC") && !fileType.equalsIgnoreCase("TEXT") && !fileType.equalsIgnoreCase("PARQUET")){
                String message = "HdfsWriter插件目前只支持TEXT、ORC和PARQUET三种格式的文件,请将filetype选项的值配置为TEXT、ORC或者PARQUET";
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE, message);
            }
            //path
            this.path = this.writerSliceConfig.getNecessaryValue(Key.PATH, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
            if(!path.startsWith("/")){
                String message = String.format("请检查参数path:[%s],需要配置为绝对路径", path);
                LOG.error(message);
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE, message);
            }else if(path.contains("*") || path.contains("?")){
                String message = String.format("请检查参数path:[%s],不能包含*,?等特殊字符", path);
                LOG.error(message);
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE, message);
            }
            //fileName
            this.fileName = this.writerSliceConfig.getNecessaryValue(Key.FILE_NAME, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
            //columns check
            this.columns = this.writerSliceConfig.getListConfiguration(Key.COLUMN);
            if (null == columns || columns.size() == 0) {
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.REQUIRED_VALUE, "您需要指定 columns");
            }else{
                for (Configuration eachColumnConf : columns) {
                    eachColumnConf.getNecessaryValue(Key.NAME, Hdfs3xWriterErrorCode.COLUMN_REQUIRED_VALUE);
                    eachColumnConf.getNecessaryValue(Key.TYPE, Hdfs3xWriterErrorCode.COLUMN_REQUIRED_VALUE);
                }
            }
            //writeMode check
            this.writeMode = this.writerSliceConfig.getNecessaryValue(Key.WRITE_MODE, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
            writeMode = writeMode.toLowerCase().trim();
            Set<String> supportedWriteModes = Sets.newHashSet("append", "nonconflict", "truncate");
            if (!supportedWriteModes.contains(writeMode)) {
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                        String.format("仅支持append, nonConflict, truncate三种模式, 不支持您配置的 writeMode 模式 : [%s]",
                                writeMode));
            }
            this.writerSliceConfig.set(Key.WRITE_MODE, writeMode);
            //fieldDelimiter check
            this.fieldDelimiter = this.writerSliceConfig.getString(Key.FIELD_DELIMITER,null);
            if(null == fieldDelimiter){
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.REQUIRED_VALUE,
                        String.format("您提供配置文件有误，[%s]是必填参数.", Key.FIELD_DELIMITER));
            }else if(1 != fieldDelimiter.length()){
                // warn: if have, length must be one
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                        String.format("仅仅支持单字符切分, 您配置的切分为 : [%s]", fieldDelimiter));
            }
            //compress check
            this.compress  = this.writerSliceConfig.getString(Key.COMPRESS,null);
            if(fileType.equalsIgnoreCase("TEXT")){
                Set<String> textSupportedCompress = Sets.newHashSet("GZIP", "BZIP2");
                //用户可能配置的是compress:"",空字符串,需要将compress设置为null
                if(StringUtils.isBlank(compress) ){
                    this.writerSliceConfig.set(Key.COMPRESS, null);
                }else {
                    compress = compress.toUpperCase().trim();
                    if(!textSupportedCompress.contains(compress) ){
                        throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                                String.format("目前TEXT FILE仅支持GZIP、BZIP2 两种压缩, 不支持您配置的 compress 模式 : [%s]",
                                        compress));
                    }
                }
            }else if(fileType.equalsIgnoreCase("ORC")){
                Set<String> orcSupportedCompress = Sets.newHashSet("NONE", "SNAPPY");
                if(null == compress){
                    this.writerSliceConfig.set(Key.COMPRESS, "NONE");
                }else {
                    compress = compress.toUpperCase().trim();
                    if(!orcSupportedCompress.contains(compress)){
                        throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                                String.format("目前ORC FILE仅支持SNAPPY压缩, 不支持您配置的 compress 模式 : [%s]",
                                        compress));
                    }
                }

            }else if(fileType.equalsIgnoreCase("PARQUET")){
                Set<String> orcSupportedCompress = Sets.newHashSet("UNCOMPRESSED", "SNAPPY");
                if(null == compress){
                    this.writerSliceConfig.set(Key.COMPRESS, "UNCOMPRESSED");
                }else {
                    compress = compress.toUpperCase().trim();
                    if(!orcSupportedCompress.contains(compress)){
                        throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                                String.format("目前PARQUET FILE仅支持SNAPPY压缩, 不支持您配置的 compress 模式 : [%s]",
                                        compress));
                    }
                }

            }
            //Kerberos check
            Boolean haveKerberos = this.writerSliceConfig.getBool(Key.HAVE_KERBEROS, false);
            if(haveKerberos) {
                this.writerSliceConfig.getNecessaryValue(Key.KERBEROS_KEYTAB_FILE_PATH, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
                this.writerSliceConfig.getNecessaryValue(Key.KERBEROS_PRINCIPAL, Hdfs3xWriterErrorCode.REQUIRED_VALUE);
            }
            // encoding check
            this.encoding = this.writerSliceConfig.getString(Key.ENCODING,Constant.DEFAULT_ENCODING);
            try {
                encoding = encoding.trim();
                this.writerSliceConfig.set(Key.ENCODING, encoding);
                Charsets.toCharset(encoding);
            } catch (Exception e) {
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                        String.format("不支持您配置的编码格式:[%s]", encoding), e);
            }
        }

        @Override
        public void prepare() {
            //若路径已经存在，检查path是否是目录
            if(hdfsHelper.isPathexists(path)){
                if(!hdfsHelper.isPathDir(path)){
                    throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                            String.format("您配置的path: [%s] 不是一个合法的目录, 请您注意文件重名, 不合法目录名等情况.",
                                    path));
                }
                //根据writeMode对目录下文件进行处理
                Path[] existFilePaths = hdfsHelper.hdfsDirList(path,fileName);
                boolean isExistFile = false;
                if(existFilePaths.length > 0){
                    isExistFile = true;
                }
                /**
                 if ("truncate".equals(writeMode) && isExistFile ) {
                 LOG.info(String.format("由于您配置了writeMode truncate, 开始清理 [%s] 下面以 [%s] 开头的内容",
                 path, fileName));
                 hdfsHelper.deleteFiles(existFilePaths);
                 } else
                 */
                if ("append".equalsIgnoreCase(writeMode)) {
                    LOG.info(String.format("由于您配置了writeMode append, 写入前不做清理工作, [%s] 目录下写入相应文件名前缀  [%s] 的文件",
                            path, fileName));
                } else if ("nonconflict".equalsIgnoreCase(writeMode) && isExistFile) {
                    LOG.info(String.format("由于您配置了writeMode nonConflict, 开始检查 [%s] 下面的内容", path));
                    List<String> allFiles = new ArrayList<String>();
                    for (Path eachFile : existFilePaths) {
                        allFiles.add(eachFile.toString());
                    }
                    LOG.error(String.format("冲突文件列表为: [%s]", StringUtils.join(allFiles, ",")));
                    throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                            String.format("由于您配置了writeMode nonConflict,但您配置的path: [%s] 目录不为空, 下面存在其他文件或文件夹.", path));
                }else if ("truncate".equalsIgnoreCase(writeMode) && isExistFile) {
                    LOG.info(String.format("由于您配置了writeMode truncate,  [%s] 下面的内容将被覆盖重写", path));
                    hdfsHelper.deleteFiles(existFilePaths);
                }
            }else{
                throw DataXException.asDataXException(Hdfs3xWriterErrorCode.ILLEGAL_VALUE,
                        String.format("您配置的path: [%s] 不存在, 请先在hive端创建对应的数据库和表.", path));
            }
        }

        @Override
        public void post() {
            hdfsHelper.renameFile(tmpFiles, endFiles);
        }

        @Override
        public void destroy() {
            hdfsHelper.closeFileSystem();
        }

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            LOG.info("begin do split...");
            List<Configuration> writerSplitConfigs = new ArrayList<Configuration>();
            String filePrefix = fileName;

            Set<String> allFiles = new HashSet<String>();

            //获取该路径下的所有已有文件列表
            if(hdfsHelper.isPathexists(path)){
                allFiles.addAll(Arrays.asList(hdfsHelper.hdfsDirList(path)));
            }

            String fileSuffix;
            //临时存放路径
            String storePath =  buildTmpFilePath(this.path);
            //最终存放路径
            String endStorePath = buildFilePath();
            this.path = endStorePath;
            for (int i = 0; i < mandatoryNumber; i++) {
                // handle same file name

                Configuration splitedTaskConfig = this.writerSliceConfig.clone();
                String fullFileName = null;
                String endFullFileName = null;

                fileSuffix = UUID.randomUUID().toString().replace('-', '_');

                fullFileName = String.format("%s%s%s__%s", defaultFS, storePath, filePrefix, fileSuffix);
                endFullFileName = String.format("%s%s%s__%s", defaultFS, endStorePath, filePrefix, fileSuffix);

                while (allFiles.contains(endFullFileName)) {
                    fileSuffix = UUID.randomUUID().toString().replace('-', '_');
                    fullFileName = String.format("%s%s%s__%s", defaultFS, storePath, filePrefix, fileSuffix);
                    endFullFileName = String.format("%s%s%s__%s", defaultFS, endStorePath, filePrefix, fileSuffix);
                }
                allFiles.add(endFullFileName);

                //设置临时文件全路径和最终文件全路径
                if("GZIP".equalsIgnoreCase(this.compress)){
                    this.tmpFiles.add(fullFileName + ".gz");
                    this.endFiles.add(endFullFileName + ".gz");
                }else if("BZIP2".equalsIgnoreCase(compress)){
                    this.tmpFiles.add(fullFileName + ".bz2");
                    this.endFiles.add(endFullFileName + ".bz2");
                }else{
                    this.tmpFiles.add(fullFileName);
                    this.endFiles.add(endFullFileName);
                }

                splitedTaskConfig
                        .set(Key.FILE_NAME,
                                fullFileName);

                LOG.info(String.format("splited write file name:[%s]",
                        fullFileName));

                writerSplitConfigs.add(splitedTaskConfig);
            }
            LOG.info("end do split.");
            return writerSplitConfigs;
        }

        private String buildFilePath() {
            boolean isEndWithSeparator = false;
            switch (IOUtils.DIR_SEPARATOR) {
                case IOUtils.DIR_SEPARATOR_UNIX:
                    isEndWithSeparator = this.path.endsWith(String
                            .valueOf(IOUtils.DIR_SEPARATOR));
                    break;
                case IOUtils.DIR_SEPARATOR_WINDOWS:
                    isEndWithSeparator = this.path.endsWith(String
                            .valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                    break;
                default:
                    break;
            }
            if (!isEndWithSeparator) {
                this.path = this.path + IOUtils.DIR_SEPARATOR;
            }
            return this.path;
        }

        /**
         * 创建临时目录
         * @param userPath
         * @return
         */
        private String buildTmpFilePath(String userPath) {
            String tmpFilePath;
            boolean isEndWithSeparator = false;
            switch (IOUtils.DIR_SEPARATOR) {
                case IOUtils.DIR_SEPARATOR_UNIX:
                    isEndWithSeparator = userPath.endsWith(String
                            .valueOf(IOUtils.DIR_SEPARATOR));
                    break;
                case IOUtils.DIR_SEPARATOR_WINDOWS:
                    isEndWithSeparator = userPath.endsWith(String
                            .valueOf(IOUtils.DIR_SEPARATOR_WINDOWS));
                    break;
                default:
                    break;
            }
            String tmpSuffix;
            tmpSuffix = UUID.randomUUID().toString().replace('-', '_');
            if (!isEndWithSeparator) {
                tmpFilePath = String.format("%s__%s%s", userPath, tmpSuffix, IOUtils.DIR_SEPARATOR);
            }else if("/".equals(userPath)){
                tmpFilePath = String.format("%s__%s%s", userPath, tmpSuffix, IOUtils.DIR_SEPARATOR);
            }else{
                tmpFilePath = String.format("%s__%s%s", userPath.substring(0,userPath.length()-1), tmpSuffix, IOUtils.DIR_SEPARATOR);
            }
            while(hdfsHelper.isPathexists(tmpFilePath)){
                tmpSuffix = UUID.randomUUID().toString().replace('-', '_');
                if (!isEndWithSeparator) {
                    tmpFilePath = String.format("%s__%s%s", userPath, tmpSuffix, IOUtils.DIR_SEPARATOR);
                }else if("/".equals(userPath)){
                    tmpFilePath = String.format("%s__%s%s", userPath, tmpSuffix, IOUtils.DIR_SEPARATOR);
                }else{
                    tmpFilePath = String.format("%s__%s%s", userPath.substring(0,userPath.length()-1), tmpSuffix, IOUtils.DIR_SEPARATOR);
                }
            }
            return tmpFilePath;
        }
    }

    public static class Task extends Writer.Task {
        private static final Logger LOG = LoggerFactory.getLogger(Task.class);

        private Configuration writerSliceConfig;

        private String defaultFS;
        private String fileType;
        private String fileName;

        private HdfsHelper hdfsHelper = null;

        @Override
        public void init() {
            this.writerSliceConfig = this.getPluginJobConf();

            this.defaultFS = this.writerSliceConfig.getString(Key.DEFAULT_FS);
            this.fileType = this.writerSliceConfig.getString(Key.FILE_TYPE);
            //得当的已经是绝对路径，eg：hdfs://10.101.204.12:9000/user/hive/warehouse/writer.db/text/test.textfile
            this.fileName = this.writerSliceConfig.getString(Key.FILE_NAME);

            hdfsHelper = new HdfsHelper();
            hdfsHelper.getFileSystem(defaultFS, writerSliceConfig);
        }

        @Override
        public void prepare() {

        }

        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            LOG.info("begin do write...");
            LOG.info(String.format("write to file : [%s]", this.fileName));
            if(fileType.equalsIgnoreCase("TEXT")){
                //写TEXT FILE
                hdfsHelper.textFileStartWrite(lineReceiver,this.writerSliceConfig, this.fileName,
                        this.getTaskPluginCollector());
            }else if(fileType.equalsIgnoreCase("ORC")){
                //写ORC FILE
                hdfsHelper.orcFileStartWrite(lineReceiver,this.writerSliceConfig, this.fileName,
                        this.getTaskPluginCollector());
            }else if(fileType.equalsIgnoreCase("PARQUET")){
                //写PARQUET FILE
                hdfsHelper.parquetFileStartWrite(lineReceiver,this.writerSliceConfig, this.fileName,
                        this.getTaskPluginCollector());
            }

            LOG.info("end do write");
        }

        @Override
        public void post() {

        }

        @Override
        public void destroy() {

        }
    }
}
