package com.alibaba.datax.plugin.writer.hivewriter;

import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.rdbms.writer.Constant;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.datax.plugin.rdbms.writer.CommonRdbmsWriter;
import com.alibaba.datax.plugin.rdbms.writer.Key;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class HiveWriter extends Writer {
    private static final DataBaseType DATABASE_TYPE = DataBaseType.Hive;

    public static class Job extends Writer.Job {
        private static final Logger LOG = LoggerFactory.getLogger(Job.class);

        private Configuration originalConfig = null;
        private CommonRdbmsWriter.Job commonRdbmsWriterJob;

        @Override
        public void preCheck(){
            this.init();
            this.commonRdbmsWriterJob.writerPreCheck(this.originalConfig, DATABASE_TYPE);
        }

        @Override
        public void init() {
            Configuration originalJson = super.getPluginJobConf();
            //集群配置写死
            List<JSONObject> connList = originalJson.getList(Constant.CONN_MARK, JSONObject.class);
            for (JSONObject connection : connList) {
                String jdbcUrl = connection.getString(Key.JDBC_URL);

                if (StringUtils.isEmpty(jdbcUrl)) {
                    continue;
                }

                String database = "";
                try {
                    database = jdbcUrl.split(";")[0].split("//")[1].split("/")[1].trim();
                } catch (Exception e) {
                    LOG.warn("cannot get database in jdbcUrl, set database to empty");
                }

                String newJdbcUrl = "jdbc:hive2://dev-cdp01:2181,dev-cdp02:2181,dev-cdp03:2181/" + database + ";serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2";
                connection.put(Key.JDBC_URL, newJdbcUrl);
            }
            originalJson.set(Constant.CONN_MARK, connList);
            LOG.info("replace jdbcUrl in HiveWriter: " + originalJson);

            this.originalConfig = originalJson;

            DFSUtil dfsUtil = new DFSUtil(this.originalConfig);

            this.commonRdbmsWriterJob = new CommonRdbmsWriter.Job(DATABASE_TYPE);
            this.commonRdbmsWriterJob.init(this.originalConfig);
            LOG.info("init() ok and end...");
        }

        // 一般来说，是需要推迟到 task 中进行pre 的执行（单表情况例外）
        @Override
        public void prepare() {
            this.commonRdbmsWriterJob.prepare(this.originalConfig);
        }

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            return this.commonRdbmsWriterJob.split(this.originalConfig, mandatoryNumber);
        }

        // 一般来说，是需要推迟到 task 中进行post 的执行（单表情况例外）
        @Override
        public void post() {
            this.commonRdbmsWriterJob.post(this.originalConfig);
        }

        @Override
        public void destroy() {
            this.commonRdbmsWriterJob.destroy(this.originalConfig);
        }

    }

    public static class Task extends Writer.Task {
        private Configuration writerSliceConfig;
        private CommonRdbmsWriter.Task commonRdbmsWriterTask;

        @Override
        public void init() {
            this.writerSliceConfig = super.getPluginJobConf();
            DFSUtil dfsUtil = new DFSUtil(this.writerSliceConfig);
            this.commonRdbmsWriterTask = new CommonRdbmsWriter.Task(DATABASE_TYPE);
            this.commonRdbmsWriterTask.init(this.writerSliceConfig);
        }

        @Override
        public void prepare() {
            this.commonRdbmsWriterTask.prepare(this.writerSliceConfig);
        }

        @Override
        public void startWrite(RecordReceiver recordReceiver) {
            this.commonRdbmsWriterTask.startWrite(recordReceiver, this.writerSliceConfig,
                    super.getTaskPluginCollector());
        }

        @Override
        public void post() {
            this.commonRdbmsWriterTask.post(this.writerSliceConfig);
        }

        @Override
        public void destroy() {
            this.commonRdbmsWriterTask.destroy(this.writerSliceConfig);
        }

        @Override
        public boolean supportFailOver(){
            String writeMode = writerSliceConfig.getString(Key.WRITE_MODE);
            return "replace".equalsIgnoreCase(writeMode);
        }

    }


}
