# DataX HiveWriter


---


## 1 快速介绍

HiveWriter 插件实现了写入数据到 Hive 主库的目的表的功能。在底层实现上， HiveWriter 通过 JDBC 连接远程 Hive 数据库，并执行相应的 insert into ... 或者 ( replace into ...) 的 sql 语句将数据写入 Hive，内部会分批次提交入库，需要数据库本身采用 innodb 引擎。

## 3 功能说明

### 3.1 配置样例

* 这里使用一份从内存产生到 Hive 导入的数据。

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
						"column": [
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
					"name": "hivewriter",
					"parameter": {
						"column": [
							"id",
							"name",
							"dt"
						],
						"connection": [
							{
								"jdbcUrl": "jdbc:hive2://hadoop.com:10000/ods;principal=hive/_HOST@XXXX.COM",
								"table": [
									"hivetest.datax1"
								]
							}
						],
						"defaultFS": "hdfs://nameservice1",
						"haveKerberos": true,
						"kerberosKeytabFilePath": "/home/user.keytab",
						"kerberosPrincipal": "user@XXXX.COM",
						"password": "*****",
						"username": "user"
					}
				}
			}
        ]
    }
}

```
