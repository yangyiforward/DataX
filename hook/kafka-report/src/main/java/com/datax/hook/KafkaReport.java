package com.datax.hook;

import com.alibaba.datax.common.spi.Hook;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yangyi5
 * @date 2023/2/23
 */
public class KafkaReport implements Hook {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaReport.class);

    @Override
    public String getName() {
        return "KafkaReport";
    }

    @Override
    public void invoke(Configuration configuration, Map<String, Number> map) {
        LOG.debug(configuration.beautify());
        LOG.debug(map.toString());
        //{writeSucceedRecords=1248,
        // readSucceedRecords=1247,
        // totalErrorBytes=0,
        // writeSucceedBytes=81477,
        // byteSpeed=0,
        // totalErrorRecords=0,
        // recordSpeed=0,
        // waitReaderTime=308600221,
        // writeReceivedBytes=81477,
        // stage=1,
        // waitWriterTime=6348796,
        // percentage=1.0,
        // totalReadRecords=1247,
        // writeReceivedRecords=1248,
        // readSucceedBytes=81477,
        // totalReadBytes=81477}

        try {
            // 从job的json配置中读取自定义的参数
            String hooks = getString(configuration.get("job.hooks"));
            String hookKey = getString(configuration.get("hookKey"));
            if (hooks.isEmpty() || hookKey.isEmpty()) {
                return;
            }
            LOG.info("datax hook start!");
            JSONArray hookList = JSON.parseArray(hooks);

            if (!"kafkaReport".equals(hookKey.split("_")[0])) {
                LOG.error("hookKey params error!");
                return;
            }

            JSONObject kafkaReporter = new JSONObject();
            for (int i=0; i<hookList.size(); i++) {
                if (hookList.getJSONObject(i).containsKey(hookKey)) {
                    kafkaReporter = hookList.getJSONObject(i).getJSONObject(hookKey);
                }
            }
            if (kafkaReporter.isEmpty()) {
                LOG.error("kafkaReport hook does not exit!");
                return;
            }

            String servers = kafkaReporter.getString("servers");
            //String servers = getString(configuration.get("job.kafkaReporter.servers"));
            String username = kafkaReporter.getString("username");
            String password = kafkaReporter.getString("password");
            String topic = kafkaReporter.getString("topic");
            String data = kafkaReporter.getString("data");
            String receiver = kafkaReporter.getString("receiver");
            if (servers.isEmpty() || topic.isEmpty() || receiver.isEmpty()) {
                LOG.error("kafkaReport hook main param is empty!");
                return;
            }

            JSONObject content = new JSONObject();
            data = replaceDataComm(data, map);
            String timeStamp = String.valueOf(System.currentTimeMillis());
            content.put("receiver", receiver);
            content.put("data", data);
            content.put("timestamp", timeStamp);

            KafkaUtil.send(servers, username, password, topic, content.toString());
        } catch (Exception e) {
            LOG.error("datax hook failed!");
            e.printStackTrace();
        }
    }

    public String getString(Object obj) {
        return null == obj ? "" : String.valueOf(obj);
    }

    private String replaceDataComm(String data, Map<String, Number> map) {
        Pattern p = Pattern.compile("\\~\\{.*?\\}");
        Matcher m = p.matcher(data);

        List<String> keys = new ArrayList<>();
        while (m.find()) {
            keys.add(m.group());
        }

        if (keys.isEmpty()) {
            LOG.debug("data中没有使用DataX特殊参数！");
        }

        for (String key : keys) {
            if (key.substring(2, key.length()-1).startsWith("counter.")) {
                String counterKey = key.substring(2, key.length()-1).replace("counter.", "");
                if (!map.isEmpty() && map.containsKey(counterKey)) {
                    String counterValue;
                    if ("writeSucceedRecords".equals(counterKey)) {
                        long val = (map.get(counterKey).longValue() - map.get("stage").longValue());
                        counterValue = Long.toString(val);
                    } else {
                        counterValue = map.get(counterKey).toString();
                    }
                    data = data.replace(key, counterValue);
                } else {
                    LOG.error("DataX counter中获取" + counterKey + "出错！请检查是否填写正确。");
                }
            } else if (key.substring(2, key.length()-1).startsWith("config.")) {
                // TODO:可使用config内参数
            } else {
                LOG.error("data中使用的特殊参数不是counter.或config.开头！");
                return null;
            }
        }

        return data;
    }
}
