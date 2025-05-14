# DataY

DataY
基于DataX开发，新增了多种数据源及功能。

# Features

DataY使用DataX作为数据同步框架，将不同数据源的同步抽象为从源头数据源读取数据的Reader插件，以及向目标端写入数据的Writer插件，理论上DataX框架可以支持任意数据源类型的数据同步工作。同时DataX插件体系作为一套生态系统, 每接入一套新数据源该新加入的数据源即可实现和现有的数据源互通。

**1.支持密码加密**

加密方式：使用DES加密算法，对json填入的数据源密码进行解密，避免暴露明文密码。

**2.支持配置中心**

1）背景

在DataX脚本中需要填写数据源、用户名、密码等通用的信息，该类信息通常为固定值，但存在变更的可能。当发生改变时，需要找出所有用到该值的脚本并逐一修改。 在不同环境中，需要对这些信息设置不同的值。 对这些通用信息需要线下人工进行管理。 因此需要使用配置中心统一管理变量，并在使用时将脚本中的变量替换为真实值。

2）使用方式

   语法：#{key,default}
   - 使用#{}来表示该变量需要从配置中心装配。
   - key为该变量的唯一id，仅支持字符、数字、下划线、中划线和点。
   - default为该变量的默认值，当无法从配置中心获取到值时，会填入该默认值。
   示例：#{mysql_1_jdbc, jdbc:mysql://10.11.111.11:3306/data}

3）环境

请设置系统变量APOLLO_CONFIG_URL、APOLLO_ACCESS_KEY_SECRET（如有），示例：
```shell
export APOLLO_CONFIG_URL=http://config.apollo.com
export APOLLO_ACCESS_KEY_SECRET=xxxxxxxx
```

**3.支持hook顺序调用**

- 添加配置信息
  
    支持单作业顺次调用多个hook插件，单个hook插件支持添加hook名称 + “_” + 数字的格式多次调用

```json
{
    "job": {
        "content": [
            {
                "reader": {
                },
                "writer": {
                }
            }
        ],
        "setting": {
        },
        "hooks":[//调用顺序为HookName_5、HookName_1、HookName
            {
                "HookName_5": {//HookName
                    "receiver":"XXX",//ProjectName
                    "data": {
                        "count": "~{counter.waitReaderTime}"//counter参数
                    },
                    "xxx": ""//Other params
                }
            },
            {
                "HookName_1": {
                    "receiver":"XXX",
                    "data": {
                        "count": "~{counter.waitReaderTime}"
                    },
                    "xxx": ""
                }
            },
            {
                "HookName": {
                    "receiver":"XXX",
                    "data": {
                        "count": "~{counter.stage}"
                    },
                    "xxx": ""
                }
            }
        ]
    }
}    
```
- 支持counter参数

  hooks配置中可以使用DataX本身提供的统计参数，使用方式为~{xxxxx}。具体参数列表如下：

| **counter参数名称** | **counter参数含义** |
| --- | --- |
| counter.byteSpeed | 处理字节速度 |
| counter.percentage | 处理百分比 |
| counter.readSucceedBytes | reader成功读取速度 |
| counter.readSucceedRecords | reader成功读取记录数 |
| counter.recordSpeed | 处理记录速度 |
| counter.stage | 分片数 |
| counter.totalErrorBytes | 整体错误记录处理速度 |
| counter.totalErrorRecords | 整体错误记录数 |
| counter.totalReadBytes | 整体读取速度 |
| counter.totalReadRecords | 整体读取记录数 |
| counter.waitReaderTime | reader等待时间 |
| counter.waitWriterTime | writer等待时间 |
| counter.writeReceivedBytes | writer接收速度 |
| counter.writeReceivedRecords | writer接收记录数 |
| counter.writeSucceedBytes | writer成功写入速度 |
| counter.writeSucceedRecords | writer成功写入记录数 |


# Support Data Channels 

DataY目前已经有了比较全面的插件体系，主流的RDBMS数据库、NOSQL、大数据计算系统都已经接入。

| **类型** | **数据源** | **Reader** | **Writer** |
| --- | --- | --- | --- |
| RDBMS 关系型数据库 | MySQL | √ |√ |
| | MySQL8 | √ |√ |
| | Oracle | √ |√ |
| | OceanBase | √ |√ |
| | SQLServer | √ |√ |
| | PostgreSQL | √ |√ |
| | Opengauss | √ |√ |
| | DRDS | √ |√ |
| | Kingbase| √ |√ |
| | Sybase| √ |√ |
| | GaussDB| √ |√ |
| | 通用RDBMS(Dameng，TiDB，DB2，EnterpriseDB)| √ |√ |
| 阿里云数仓数据存储 | ODPS | √ |√ |
| | ADS | |√ |
| | OSS | √ |√ |
| | OCS | |√ |
| 阿里云图数据库 | GDB | √ |√ |
| NoSQL数据存储 | OTS | √ |√ |
| | Hbase0.94 | √ |√ |
| | Hbase1.1 | √ |√ |
| | Phoenix4.x | √ |√ |
| | Phoenix5.x | √ |√ |
| | MongoDB | √ |√ |
| | Cassandra | √ |√ |
| 数仓数据存储 | ClickHouse | √ |√ |
| | Hive Jdbc | √ |√ |
| | sensors | √ |√ |
| 无结构化数据存储 | TxtFile | √ |√ |
| | FTP | √ |√ |
| | S3 | √ | |
| | HDFS(txt，orc) | √ |√ |
| | HDFS3x(parquet) | |√ |
| | TBDS | √ | |
| | Elasticsearch | √ |√ |
| 时间序列数据库 | OpenTSDB | √ | |
| | TSDB | √ |√ |
| | DolphinDB | √ |√ |
