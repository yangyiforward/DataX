# DataX GaussDBWriter


---


## 1 快速介绍

GaussDBWriter插件实现了写入数据到 GaussDB主库目的表的功能。在底层实现上，GaussDBWriter通过JDBC连接远程 GaussDB 数据库，并执行相应的 insert into ... sql 语句将数据写入 GaussDB，内部会分批次提交入库。

GaussDBWriter面向ETL开发工程师，他们使用GaussDBWriter从数仓导入数据到GaussDB。同时 GaussDBWriter亦可以作为数据迁移工具为DBA等用户提供服务。


## 2 实现原理

GaussDBWriter通过 DataX 框架获取 Reader 生成的协议数据，根据你配置生成相应的SQL插入语句


* `insert into...`(当主键/唯一性索引冲突时会写不进去冲突的行)

<br />

    注意：
    1. 目的表所在数据库必须是主库才能写入数据；整个任务至少需具备 insert into...的权限，是否需要其他权限，取决于你任务配置中在 preSql 和 postSql 中指定的语句。
    2. GaussDBWriter和MysqlWriter不同，不支持配置writeMode参数。


## 3 功能说明

### 3.1 配置样例

* 这里使用一份从内存产生到 GaussDBWriter导入的数据。

```json
{
    "job": {
        "setting": {
            "speed": {
                "channel": 1
            }
        },
        "content": [
            {
                 "reader": {
                    "name": "streamreader",
                    "parameter": {
                        "column" : [
                            {
                                "value": "DataX",
                                "type": "string"
                            },
                            {
                                "value": 19880808,
                                "type": "long"
                            },
                            {
                                "value": "1988-08-08 08:08:08",
                                "type": "date"
                            },
                            {
                                "value": true,
                                "type": "bool"
                            },
                            {
                                "value": "test",
                                "type": "bytes"
                            }
                        ],
                        "sliceRecordCount": 1000
                    }
                },
                "writer": {
                    "name": "gaussdbwriter",
                    "parameter": {
                        "username": "xx",
                        "password": "xx",
                        "column": [
                            "id",
                            "name"
                        ],
                        "preSql": [
                            "delete from test"
                        ],
                        "connection": [
                            {
                                "jdbcUrl": "jdbc:gaussdb://127.0.0.1:3002/datax",
                                "table": [
                                    "test"
                                ]
                            }
                        ]
                    }
                }
            }
        ]
    }
}
```


### 3.2 参数说明

* **jdbcUrl**

    * 描述：目的数据库的 JDBC 连接信息 ,jdbcUrl必须包含在connection配置单元中。

      注意：1、在一个数据库上只能配置一个值。
      2、jdbcUrl按照GaussDB官方规范，并可以填写连接附加参数信息。具体请参看GaussDB官方文档或者咨询对应 DBA。


* 必选：是 <br />

* 默认值：无 <br />

* **username**

    * 描述：目的数据库的用户名 <br />

    * 必选：是 <br />

    * 默认值：无 <br />

* **password**

    * 描述：目的数据库的密码 <br />

    * 必选：是 <br />

    * 默认值：无 <br />

* **table**

    * 描述：目的表的表名称。支持写入一个或者多个表。当配置为多张表时，必须确保所有表结构保持一致。

                 注意：table 和 jdbcUrl 必须包含在 connection 配置单元中

    * 必选：是 <br />

    * 默认值：无 <br />

* **column**

    * 描述：目的表需要写入数据的字段,字段之间用英文逗号分隔。例如: "column": ["id","name","age"]。如果要依次写入全部列，使用\*表示, 例如: "column": ["\*"]

                 注意：1、我们强烈不推荐你这样配置，因为当你目的表字段个数、类型等有改动时，你的任务可能运行不正确或者失败
                      2、此处 column 不能配置任何常量值

    * 必选：是 <br />

    * 默认值：否 <br />

* **preSql**

    * 描述：写入数据到目的表前，会先执行这里的标准语句。如果 Sql 中有你需要操作到的表名称，请使用 `@table` 表示，这样在实际执行 Sql 语句时，会对变量按照实际表名称进行替换。比如你的任务是要写入到目的端的100个同构分表(表名称为:datax_00,datax01, ... datax_98,datax_99)，并且你希望导入数据前，先对表中数据进行删除操作，那么你可以这样配置：`"preSql":["delete from @table"]`，效果是：在执行到每个表写入数据前，会先执行对应的 delete from 对应表名称 <br />

    * 必选：否 <br />

    * 默认值：无 <br />

* **postSql**

    * 描述：写入数据到目的表后，会执行这里的标准语句。（原理同 preSql ） <br />

    * 必选：否 <br />

    * 默认值：无 <br />

* **batchSize**

    * 描述：一次性批量提交的记录数大小，该值可以极大减少DataX与GaussDB的网络交互次数，并提升整体吞吐量。但是该值设置过大可能会造成DataX运行进程OOM情况。<br />

    * 必选：否 <br />

    * 默认值：1024 <br />

### 3.3 类型转换

目前 GaussDBWriter支持大部分 GaussDB类型，但也存在部分没有支持的情况，请注意检查你的类型。

下面列出 GaussDBWriter针对 GaussDB类型转换列表:

| DataX 内部类型| GaussDB 数据类型    |
| -------- | -----  |
| Long     |bigint, bigserial, integer, smallint, serial |
| Double   |double precision, money, numeric, real |
| String   |varchar, char, text, bit|
| Date     |date, time, timestamp |
| Boolean  |bool|
| Bytes    |bytea|

## 4 性能报告

### 4.1 环境准备

#### 4.1.1 数据特征
建表语句：

create table pref_test(
id serial,
a_bigint bigint,
a_bit bit(10),
a_boolean boolean,
a_char character(5),
a_date date,
a_double double precision,
a_integer integer,
a_money money,
a_num numeric(10,2),
a_real real,
a_smallint smallint,
a_text text,
a_time time,
a_timestamp timestamp
)

#### 4.1.2 机器参数

* 执行DataX的机器参数为:
    1. cpu: 16核 Intel(R) Xeon(R) CPU E5620  @ 2.40GHz
    2. mem: MemTotal: 24676836kB    MemFree: 6365080kB
    3. net: 百兆双网卡