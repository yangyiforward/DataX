
# HiveReader 插件文档


___



## 1 快速介绍

HiveReader插件实现了从Hive读取数据。在底层实现上，HiveReader通过JDBC连接远程Hive，并执行相应的sql语句将数据从Hive中SELECT出来。


## 3 功能说明

### 3.1 配置样例

* 配置一个从Mysql数据库同步抽取数据到本地的作业:

```
{
    "job": {
        "setting": {
            "speed": {
                 "channel": 3
            },
            "errorLimit": {
                "record": 0,
                "percentage": 0.02
            }
        },
        "content": [
            {
                "reader": {
					"name": "hivereader",
					"parameter": {
						"connection": [
							{
								"jdbcUrl": ["jdbc:hive2://hadoop.com:10000/ods;principal=hive/_HOST@XXXX.COM"],
								"querySql": ["select custid, custname from ods.test where custid < '800100000010'"],
							}
						],
						"defaultFS": "hdfs://hadoop.com:8020",
						"haveKerberos": true,
						"kerberosKeytabFilePath": "/home/user.keytab",
						"kerberosPrincipal": "user@XXXX.COM",
						"password": "*****",
						"username": "user",
					}
				},
               "writer": {
                    "name": "streamwriter",
                    "parameter": {
                        "print":true
                    }
                }
            }
        ]
    }
}

```


