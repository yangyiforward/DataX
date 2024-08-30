package com.cicc.datax.hook;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;


/**
 * @author yangyi5
 * @date 2023/2/23
 */
public class KafkaUtil {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaUtil.class);

    /**
     *
     * @param servers         服务器地址
     * @param username        用户名
     * @param password        密码
     * @param topic           主题
     * @param content         消息
     */
    public static void send(String servers, String username, String password, String topic, String content) {
        if(isEmpty(servers) || isEmpty(username) || isEmpty(password) || isEmpty(topic)){
            LOG.error("please check kafkaReport hook params");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", servers);
            //server地址和端口号.
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            // sasl.jaas.config的配置, 结尾分号必不可少.
            props.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username='" + username + "' password='" + decode(password) + "';");
            //设置client.properties参数
            props.setProperty("security.protocol", "SASL_PLAINTEXT");
            props.setProperty("sasl.mechanism", "SCRAM-SHA-256");

            KafkaProducer<String, String> producer = new KafkaProducer<>(props);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, content);
            producer.send(record);

            producer.close();
            LOG.info("send kafka message success!");
        }catch (Exception e){
            LOG.error("send kafka message error: ", e);
        }
    }

    public static boolean isEmpty(String value) {
        return null == value || "".equals(value);
    }

    private static String decode(String encodeContent) {
        try	{
            //Base64 ALGORITHM
            //String decodeContent = new String(Base64.getDecoder().decode(encodeContent),"UTF-8");

            //DES ALGORITHM
            String originKey = "vua]28^N";
            Key key = getSecretKey(originKey);
            Cipher cipher = Cipher.getInstance("des");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decode = Base64.getDecoder().decode(encodeContent);
            byte[] decipherByte = cipher.doFinal(decode);
            String decodeContent = new String(decipherByte);

            return decodeContent;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static SecretKey getSecretKey(final String key) {
        try {
            DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
            SecretKeyFactory instance = SecretKeyFactory.getInstance("des");
            SecretKey secretKey = instance.generateSecret(desKeySpec);

            return secretKey;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "10.21.237.78:9183,10.21.237.78:9184,10.21.237.78:9185");
        props.put("group.id", "test");
        //server地址和端口号.
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // sasl.jaas.config的配置, 结尾分号必不可少.
        props.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"dipper\" password=\"dipper2302@Kafka\";");
        //设置client.properties参数
        props.setProperty("security.protocol", "SASL_PLAINTEXT");
        props.setProperty("sasl.mechanism", "SCRAM-SHA-256");
        String topicName = "dipperTest";

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        //Kafka Consumer subscribes list of topics here.
        consumer.subscribe(Collections.singletonList(topicName));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                // print the offset,key and value for the consumer records.
                System.out.printf("offset = %d, key = %s, value = %s\n", record.offset(), record.key(), record.value());
            }
        }
    }
}
