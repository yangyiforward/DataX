# datax-sensors-plugins
datax sensors的reader插件



eg:

```json
{
    "name": "kudureader",
    "parameter": {
        "connection": [
            {
                "jdbcUrl": ["jdbc:hive2://10.111.111.11:1050/rawdata;auth=noSasl"],
                "querySql": ["select id,first_id,second_id,$first_referrer,$first_search_keyword,$first_browser_language,$first_browser_charset,$first_visit_time,$first_referrer_host,$first_traffic_source_type from users /*SA(it_pangu_prd)*/"]
            }
        ],
        "password": "11111",
        "username": "admin"
    }
}
```

必须参数：

```json
        {
    "name": "sensorsreader",
    "parameter": {
        "username": "",
        "password": "",
        "column": [],
        "connection": [
            {
                "jdbcUrl": [],
                "table": []
            }
        ],
        "where": ""
    }
}
```
