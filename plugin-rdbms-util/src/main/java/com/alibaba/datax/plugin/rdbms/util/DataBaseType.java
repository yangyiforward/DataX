package com.alibaba.datax.plugin.rdbms.util;

import com.alibaba.datax.common.exception.DataXException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * refer:http://blog.csdn.net/ring0hx/article/details/6152528
 */
public enum DataBaseType {
    //in use
    MySql("mysql", "com.mysql.jdbc.Driver"),
    MySql8("mysql", "com.mysql.cj.jdbc.Driver"),
    DRDS("drds", "com.mysql.jdbc.Driver"),
    Oracle("oracle", "oracle.jdbc.OracleDriver"),
    SQLServer("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    PostgreSQL("postgresql", "org.postgresql.Driver"),
    RDBMS("rdbms", "com.alibaba.datax.plugin.rdbms.util.DataBaseType"),
    ADS("ads","com.mysql.jdbc.Driver"),
    ClickHouse("clickhouse", "ru.yandex.clickhouse.ClickHouseDriver"),
    KingbaseES("kingbasees", "com.kingbase8.Driver"),
    OceanBase("oceanbase", "com.alipay.oceanbase.jdbc.Driver"),
    Hive("hive","org.apache.hive.jdbc.HiveDriver"),
    TBDS("tbds","org.apache.hive.jdbc.HiveDriver"),
    Kudu("kudu","org.apache.hive.jdbc.HiveDriver"),
    //Sybase("sybase","com.sybase.jdbc4.jdbc.SybDriver"),
    Sybase("sybase","net.sourceforge.jtds.jdbc.Driver"),
    OpenGauss("opengauss","org.postgresql.Driver"),
    DolphinDb("dolphindb","com.dolphindb.jdbc.Driver"),
    GaussDb("gaussdb","com.huawei.gaussdb.jdbc.Driver"),
    //not used
    Oscar("oscar", "com.oscar.Driver");


    private String typeName;
    private String driverClassName;

    DataBaseType(String typeName, String driverClassName) {
        this.typeName = typeName;
        this.driverClassName = driverClassName;
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    public String appendJDBCSuffixForReader(String jdbc) {
        String result = jdbc;
        String suffix = null;
        switch (this) {
            case MySql:
            case DRDS:
            case OceanBase:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull&tinyInt1isBit=false&rewriteBatchedStatements=true";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case MySql8:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=CONVERT_TO_NULL&tinyInt1isBit=false&rewriteBatchedStatements=true";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case Oracle:
            case SQLServer:
            case PostgreSQL:
            case ClickHouse:
            case RDBMS:
            case KingbaseES:
            case Oscar:
            case Hive:
            case TBDS:
            case Kudu:
            case Sybase:
            case OpenGauss:
            case DolphinDb:
            case GaussDb:
                break;
            default:
                throw DataXException.asDataXException(DBUtilErrorCode.UNSUPPORTED_TYPE, "unsupported database type.");
        }

        return result;
    }

    public String appendJDBCSuffixForWriter(String jdbc) {
        String result = jdbc;
        String suffix = null;
        switch (this) {
            case MySql:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true&tinyInt1isBit=false";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case MySql8:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=CONVERT_TO_NULL&rewriteBatchedStatements=true&tinyInt1isBit=false";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case DRDS:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case Oracle:
            case SQLServer:
            case PostgreSQL:
            case ClickHouse:
            case RDBMS:
            case KingbaseES:
            case Oscar:
            case Hive:
            case Sybase:
            case OpenGauss:
            case DolphinDb:
            case GaussDb:
                break;
            case OceanBase:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull&tinyInt1isBit=false&rewriteBatchedStatements=true";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            default:
                throw DataXException.asDataXException(DBUtilErrorCode.UNSUPPORTED_TYPE, "unsupported database type.");
        }

        return result;
    }

    public String formatPk(String splitPk) {
        String result = splitPk;

        switch (this) {
            case MySql:
            case MySql8:
            case Oracle:
                if (splitPk.length() >= 2 && splitPk.startsWith("`") && splitPk.endsWith("`")) {
                    result = splitPk.substring(1, splitPk.length() - 1).toLowerCase();
                }
                break;
            case SQLServer:
                if (splitPk.length() >= 2 && splitPk.startsWith("[") && splitPk.endsWith("]")) {
                    result = splitPk.substring(1, splitPk.length() - 1).toLowerCase();
                }
                break;
            case PostgreSQL:
            case KingbaseES:
            case Oscar:
            case OpenGauss:
            case DolphinDb:
            case GaussDb:
                break;
            default:
                throw DataXException.asDataXException(DBUtilErrorCode.UNSUPPORTED_TYPE, "unsupported database type.");
        }

        return result;
    }


    public String quoteColumnName(String columnName) {
        String result = columnName;

        switch (this) {
            case MySql:
            case MySql8:
                result = "`" + columnName.replace("`", "``") + "`";
                break;
            case SQLServer:
                result = "[" + columnName + "]";
                break;
            case Oracle:
            case PostgreSQL:
            case KingbaseES:
            case Oscar:
            case OpenGauss:
            case DolphinDb:
            case GaussDb:
                break;
            default:
                throw DataXException.asDataXException(DBUtilErrorCode.UNSUPPORTED_TYPE, "unsupported database type");
        }

        return result;
    }

    public String quoteTableName(String tableName) {
        String result = tableName;

        switch (this) {
            case MySql:
            case MySql8:
                result = "`" + tableName.replace("`", "``") + "`";
                break;
            case Oracle:
            case SQLServer:
            case PostgreSQL:
            case KingbaseES:
            case Oscar:
            case OpenGauss:
            case DolphinDb:
            case GaussDb:
                break;
            default:
                throw DataXException.asDataXException(DBUtilErrorCode.UNSUPPORTED_TYPE, "unsupported database type");
        }

        return result;
    }

    private static final Pattern mysqlPattern = Pattern.compile("jdbc:mysql://(.+):\\d+/.+");
    private static final Pattern oraclePattern = Pattern.compile("jdbc:oracle:thin:@(.+):\\d+:.+");

    /**
     * 注意：目前只实现了从 mysql/oracle 中识别出ip 信息.未识别到则返回 null.
     */
    public static String parseIpFromJdbcUrl(String jdbcUrl) {
        Matcher mysql = mysqlPattern.matcher(jdbcUrl);
        if (mysql.matches()) {
            return mysql.group(1);
        }
        Matcher oracle = oraclePattern.matcher(jdbcUrl);
        if (oracle.matches()) {
            return oracle.group(1);
        }
        return null;
    }
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

}
