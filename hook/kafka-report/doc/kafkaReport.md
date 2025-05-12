支持将自定义消息发送至kafka，hooks内json结构为：
```json
{
    "kafkaReport": {
        "servers": "#{kafka_1_url}",//推荐使用配置中心
        "username": "#{kafka_1_user}",
        "password": "#{kafka_1_password}",
        "topic": "Test",
        "receiver":"department",
        "data": {
            "db": "test",
            "tableName": "mark_test",
            "startDate": "20220111",
            "endDate": "20220222",
            "tag": "tag",
            "count": "~{counter.writeSucceedRecords}"
        }
    }
}
```

| **参数名称** | **类型** | **是否必填** | **含义** |
| --- | --- | --- | --- |
| servers | String | 是 | kafka服务地址 |
| username | String | 否 | 用户名 |
| password | String | 否 | 密码 |
| topic | String | 是 | 主题 |
| receiver | String | 是 | 项目名称 |
| data | json | 否 | 自定义消息，内容可根据需求自定义 |

最终发送的消息体结构为：
```json
{"receiver":"department","data":"{\"count\":\"1066\",\"db\":\"test\",\"endDate\":\"20220222\",\"startDate\":\"20220111\",\"tableName\":\"mark_test\"}","timestamp":"1678311013871"}
```