# DataX

DataX of CICC


# Features

DataX本身作为数据同步框架，将不同数据源的同步抽象为从源头数据源读取数据的Reader插件，以及向目标端写入数据的Writer插件，理论上DataX框架可以支持任意数据源类型的数据同步工作。同时DataX插件体系作为一套生态系统, 每接入一套新数据源该新加入的数据源即可实现和现有的数据源互通。



# Support Data Channels 

DataX目前已经有了比较全面的插件体系，主流的RDBMS数据库、NOSQL、大数据计算系统都已经接入，目前支持数据如下图

| 类型           | 数据源        | Reader(读) | Writer(写) |文档|
| ------------ | ---------- | :-------: | :-------: |:-------: |
| RDBMS 关系型数据库 | MySQL      |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/mysqlreader/doc/mysqlreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/mysqlwriter/doc/mysqlwriter.md)|
|              | Mysql8     |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/mysql8xreader/doc/mysql8xreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/mysql8xwriter/doc/mysql8xwriter.md)|
|              | Oracle     |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/oraclereader/doc/oraclereader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/oraclewriter/doc/oraclewriter.md)|
|              | OceanBase  |     √     |     √     |[读](https://open.oceanbase.com/docs/community/oceanbase-database/V3.1.0/use-datax-to-full-migration-data-to-oceanbase) 、[写](https://open.oceanbase.com/docs/community/oceanbase-database/V3.1.0/use-datax-to-full-migration-data-to-oceanbase)|
|              | SQLServer  |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/sqlserverreader/doc/sqlserverreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/sqlserverwriter/doc/sqlserverwriter.md)|
|              | PostgreSQL |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/postgresqlreader/doc/postgresqlreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/postgresqlwriter/doc/postgresqlwriter.md)|
|              | DRDS |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/drdsreader/doc/drdsreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/drdswriter/doc/drdswriter.md)|
|              | Sybase |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/sybasereader/doc/sybasereader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/sybasewriter/doc/sybasewriter.md)|
|              | 通用RDBMS(支持所有关系型数据库)         |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/rdbmsreader/doc/rdbmsreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/rdbmswriter/doc/rdbmswriter.md)|
| 阿里云数仓数据存储    | ODPS       |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/odpsreader/doc/odpsreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/odpswriter/doc/odpswriter.md)|
|              | ADS        |           |     √     |[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/adswriter/doc/adswriter.md)|
|              | OSS        |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/ossreader/doc/ossreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/osswriter/doc/osswriter.md)|
|              | OCS        |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/ocsreader/doc/ocsreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/ocswriter/doc/ocswriter.md)|
| NoSQL数据存储    | OTS        |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/otsreader/doc/otsreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/otswriter/doc/otswriter.md)|
|              | Hbase0.94  |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase094xreader/doc/hbase094xreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase094xwriter/doc/hbase094xwriter.md)|
|              | Hbase1.1   |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase11xreader/doc/hbase11xreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase11xwriter/doc/hbase11xwriter.md)|
|              | Phoenix4.x   |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase11xsqlreader/doc/hbase11xsqlreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase11xsqlwriter/doc/hbase11xsqlwriter.md)|
|              | Phoenix5.x   |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase20xsqlreader/doc/hbase20xsqlreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hbase20xsqlwriter/doc/hbase20xsqlwriter.md)|
|              | MongoDB    |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/mongodbreader/doc/mongodbreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/mongodbwriter/doc/mongodbwriter.md)|
|              | Hive JDBC  |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hivereader/doc/hivereader.md) 、[写]|
|              | Cassandra       |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/cassandrareader/doc/cassandrareader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/cassandrawriter/doc/cassandrawriter.md)|
| 无结构化数据存储     | TxtFile    |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/txtfilereader/doc/txtfilereader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/txtfilewriter/doc/txtfilewriter.md)|
|              | FTP        |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/ftpreader/doc/ftpreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/ftpwriter/doc/ftpwriter.md)|
|              | HDFS       |     √     |     √     |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hdfsreader/doc/hdfsreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/hdfswriter/doc/hdfswriter.md)|
|              | Elasticsearch       |         |     √     |[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/elasticsearchwriter/doc/elasticsearchwriter.md)|
| 时间序列数据库 | OpenTSDB | √ |  |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/opentsdbreader/doc/opentsdbreader.md)|
|  | TSDB | √ | √ |[读](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/tsdbreader/doc/tsdbreader.md) 、[写](https://gitlab.cicconline.com/dipper/dmp/toolset/datax/-/blob/master/tsdbwriter/doc/tsdbhttpwriter.md)|
