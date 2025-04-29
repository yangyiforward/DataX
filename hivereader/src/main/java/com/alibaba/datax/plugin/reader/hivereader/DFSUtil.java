package com.alibaba.datax.plugin.reader.hivereader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by mingya.wmy on 2015/8/12.
 */
public class DFSUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HiveReader.Job.class);

    private org.apache.hadoop.conf.Configuration hadoopConf;
    private Boolean haveKerberos;
    private String kerberosKeytabFilePath;
    private String kerberosPrincipal;


    private static final int DIRECTORY_SIZE_GUESS = 16 * 1024;

    public static final String HDFS_DEFAULTFS_KEY = "fs.defaultFS";
    public static final String HADOOP_SECURITY_AUTHENTICATION_KEY = "hadoop.security.authentication";


    public DFSUtil(Configuration taskConfig) {
        hadoopConf = new org.apache.hadoop.conf.Configuration();
        //io.file.buffer.size 性能参数
        //http://blog.csdn.net/yangjl38/article/details/7583374
        Configuration hadoopSiteParams = taskConfig.getConfiguration(Key.HADOOP_CONFIG);
        JSONObject hadoopSiteParamsAsJsonObject = JSON.parseObject(taskConfig.getString(Key.HADOOP_CONFIG));
        if (null != hadoopSiteParams) {
            Set<String> paramKeys = hadoopSiteParams.getKeys();
            for (String each : paramKeys) {
                hadoopConf.set(each, hadoopSiteParamsAsJsonObject.getString(each));
            }
        }
        //集群配置写死
        hadoopConf.set(HDFS_DEFAULTFS_KEY, "hdfs://dipper-dev-dp-cdp01.cicc.com:8020");

        //是否有Kerberos认证
        this.haveKerberos = true;
        if (haveKerberos) {
            this.kerberosKeytabFilePath = "/opt/soft/cdp_job_jar/multi_tenancy_keytab/dipper_etl_dev.keytab";
            this.kerberosPrincipal = "dipper_etl@CICC.COM";
            this.hadoopConf.set(HADOOP_SECURITY_AUTHENTICATION_KEY, "kerberos");
        }
        this.kerberosAuthentication(this.kerberosPrincipal, this.kerberosKeytabFilePath);

        LOG.debug(String.format("hadoopConfig details:%s", JSON.toJSONString(this.hadoopConf)));
    }

    private void kerberosAuthentication(String kerberosPrincipal, String kerberosKeytabFilePath) {
        if (haveKerberos && StringUtils.isNotBlank(this.kerberosPrincipal) && StringUtils.isNotBlank(this.kerberosKeytabFilePath)) {
            UserGroupInformation.setConfiguration(this.hadoopConf);
            try {
                UserGroupInformation.loginUserFromKeytab(kerberosPrincipal, kerberosKeytabFilePath);
            } catch (Exception e) {
                String message = String.format("kerberos认证失败,请确定kerberosKeytabFilePath[%s]和kerberosPrincipal[%s]填写正确",
                        kerberosKeytabFilePath, kerberosPrincipal);
                throw DataXException.asDataXException(HiveReaderErrorCode.KERBEROS_LOGIN_ERROR, message, e);
            }
        }
    }

}
