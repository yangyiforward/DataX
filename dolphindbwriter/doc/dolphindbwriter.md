# DolphindbWriter插件文档

## 1 快速介绍

基于 DataX 的扩展功能，dolphindbwriter 插件实现了向 DolphinDB 写入数据。

## 3 功能说明

### 3.1 配置样例

```json
{
  "job": {
    "setting": {
      "speed": {
        "channel": 1
      },
      "errorLimit": {
        "record": 0,
        "percentage": 0.02
      }
    },
    "content": [
      {
        "reader": {
          "name": "dolphindbreader",
          "parameter": {
            "username": "admin",
            "password": "11111",
            "connection": [
              {
                "querySql": ["select * from loadTable(\"dfs://db1\", \"SZQuotation\") where Tradedate >= 2022.01.04 and Tradedate <= 2022.01.07;"],
                "jdbcUrl": ["jdbc:dolphindb://10.11.111.111:100?databasePath=dfs://db1"]
              }
            ],
            "fetchSize": 0
          }
        },
          "writer": {
              "name": "dolphindbwriter",
              "parameter": {
                  "column": ["id","date","name","part"],
                  "connection": [
                      {
                          "jdbcUrl": "jdbc:dolphindb://10.11.111.111:19100?databasePath=dfs://tmp",
                          "table": ["t_tmp2"]
                      }
                  ],
                  "password": "11111",
                  "username": "admin",
              }
          }
      }
    ]
  }
}

```

### 3.3 类型转换

下表为数据对照表（其他数据类型暂不支持）

| DolphinDB类型  | 配置值             | DataX类型 |
| ------------ | --------------- | ------- |
| DOUBLE       | DT_DOUBLE       | DOUBLE  |
| FLOAT        | DT_FLOAT        | DOUBLE  |
| BOOL         | DT_BOOL         | BOOLEAN |
| DATE         | DT_DATE         | DATE    |
| DATETIME     | DT_DATETIME     | DATE    |
| TIME         | DT_TIME         | STRING  |
| TIMESTAMP    | DT_TIMESTAMP    | DATE    |
| NANOTIME     | DT_NANOTIME     | STRING  |
| NANOTIMETAMP | DT_NANOTIMETAMP | DATE    |
| MONTH        | DT_MONTH        | DATE    |
| BYTE         | DT_BYTE         | LONG    |
| LONG         | DT_LONG         | LONG    |
| SHORT        | DT_SHORT        | LONG    |
| INT          | DT_INT          | LONG    |
| UUID         | DT_UUID         | STRING  |
| STRING       | DT_STRING       | STRING  |
| BLOB         | DT_BLOB         | STRING  |
| SYMBOL       | DT_SYMBOL       | STRING  |
| COMPLEX      | DT_COMPLEX      | STRING  |
| DATEHOUR     | DT_DATEHOUR     | DATE    |
| DURATION     | DT_DURATION     | LONG    |
| INT128       | DT_INT128       | STRING  |
| IPADDR       | DT_IPADDR       | STRING  |
| MINUTE       | DT_MINUTE       | STRING  |
| MONTH        | DT_MONTH        | STRING  |
| POINT        | DT_POINT        | STRING  |
| SECOND       | DT_SECOND       | STRING  |

## 4 约束限制

略

## FAQ

略