package com.alibaba.datax.plugin.reader.hivereader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.rdbms.reader.CommonRdbmsReader;
import com.alibaba.datax.plugin.rdbms.reader.Constant;
import com.alibaba.datax.plugin.rdbms.reader.Key;
import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class HiveReader extends Reader {

    private static final DataBaseType DATABASE_TYPE = DataBaseType.Hive;

    public static class Job extends Reader.Job {
        private static final Logger LOG = LoggerFactory.getLogger(Job.class);

        private Configuration originalConfig = null;
        private DFSUtil dfsUtil = null;
        private CommonRdbmsReader.Job commonRdbmsReaderJob;

        @Override
        public void init() {
            Configuration originalJson = super.getPluginJobConf();
            //集群配置写死
            List<JSONObject> connList = originalJson.getList(Constant.CONN_MARK, JSONObject.class);
            for (JSONObject connection : connList) {
                JSONArray jdbcUrls = connection.getJSONArray(Key.JDBC_URL);
                if (CollectionUtils.isEmpty(jdbcUrls)) {
                    continue;
                }

                String jdbcUrl = jdbcUrls.getString(0);

                String database = "";
                try {
                    database = jdbcUrl.split(";")[0].split("//")[1].split("/")[1].trim();
                } catch (Exception e) {
                    LOG.warn("cannot get database in jdbcUrl, set database to empty");
                }

                List<String> newJdbcUrls = new ArrayList<>();
                newJdbcUrls.add("jdbc:hive2://dipper-uatp-dp-cdp03.cicc.com:2181,dipper-uatp-dp-cdp02.cicc.com:2181,dipper-uatp-dp-cdp05.cicc.com:2181/" + database + ";serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2");
                connection.put(Key.JDBC_URL, newJdbcUrls);
            }
            originalJson.set(Constant.CONN_MARK, connList);
            LOG.info("replace jdbcUrl in HiveReader: " + originalJson);

            this.originalConfig = originalJson;

            dealFetchSize(this.originalConfig);

            dfsUtil = new DFSUtil(this.originalConfig);
            LOG.info("init() ok and end...");

            this.commonRdbmsReaderJob = new CommonRdbmsReader.Job(DATABASE_TYPE);
            this.commonRdbmsReaderJob.init(this.originalConfig);
        }

        @Override
        public void preCheck(){
            init();
            this.commonRdbmsReaderJob.preCheck(this.originalConfig,DATABASE_TYPE);

        }

        @Override
        public List<Configuration> split(int adviceNumber) {
            return this.commonRdbmsReaderJob.split(this.originalConfig, adviceNumber);
        }

        @Override
        public void post() {
            this.commonRdbmsReaderJob.post(this.originalConfig);
        }

        @Override
        public void destroy() {
            this.commonRdbmsReaderJob.destroy(this.originalConfig);
        }

        private void dealFetchSize(Configuration originalConfig) {
            int fetchSize = originalConfig.getInt(
                    com.alibaba.datax.plugin.rdbms.reader.Constant.FETCH_SIZE,
                    com.alibaba.datax.plugin.reader.hivereader.Constant.DEFAULT_FETCH_SIZE);
            if (fetchSize < 1) {
                throw DataXException
                        .asDataXException(DBUtilErrorCode.REQUIRED_VALUE,
                                String.format("您配置的 fetchSize 有误，fetchSize:[%d] 值不能小于 1.",
                                        fetchSize));
            }
            originalConfig.set(
                    com.alibaba.datax.plugin.rdbms.reader.Constant.FETCH_SIZE,
                    fetchSize);
        }
    }

    public static class Task extends Reader.Task {

        private Configuration readerSliceConfig;
        private DFSUtil dfsUtil = null;
        private CommonRdbmsReader.Task commonRdbmsReaderTask;

        @Override
        public void init() {
            this.readerSliceConfig = super.getPluginJobConf();
            this.dfsUtil = new DFSUtil(this.readerSliceConfig);

            this.commonRdbmsReaderTask = new CommonRdbmsReader.Task(DATABASE_TYPE,super.getTaskGroupId(), super.getTaskId());
            this.commonRdbmsReaderTask.init(this.readerSliceConfig);

        }

        @Override
        public void startRead(RecordSender recordSender) {
            int fetchSize = this.readerSliceConfig
                    .getInt(com.alibaba.datax.plugin.rdbms.reader.Constant.FETCH_SIZE);

            this.commonRdbmsReaderTask.startRead(this.readerSliceConfig, recordSender,
                    super.getTaskPluginCollector(), fetchSize);
        }

        @Override
        public void post() {
            this.commonRdbmsReaderTask.post(this.readerSliceConfig);
        }

        @Override
        public void destroy() {
            this.commonRdbmsReaderTask.destroy(this.readerSliceConfig);
        }

    }

}
