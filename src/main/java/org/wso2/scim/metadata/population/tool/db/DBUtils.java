package org.wso2.scim.metadata.population.tool.db;

import org.wso2.scim.metadata.population.tool.IdentityException;
import org.wso2.scim.metadata.population.tool.util.ConfigParser;

import java.sql.*;

public class DBUtils {

    private static ConfigParser configParser;
    private static Connection connection = null;

    public static Connection getDBConnection() throws IdentityException {

        if (connection == null) {
            if (configParser == null) {
                configParser = new ConfigParser();
            }
            String url = configParser.getProperty("CONNECTION_URL");
            String username = configParser.getProperty("CONNECTION_USERNAME");
            String password = configParser.getProperty("CONNECTION_PASSWORD");
            String driverClass = configParser.getProperty("CONNECTION_DRIVERCLASS");
            String driverLocation = configParser.getProperty("CONNECTION_JDBCDRIVER");

            DBConnection.loadDBDriver(driverLocation, driverClass);
            connection = DBConnection.getConnection(url, username, password);
        }
        return connection;
    }

    public static void closeAllConnections(Connection dbConnection, ResultSet rs, PreparedStatement prepStmt) {

        closeResultSet(rs);
        closeStatement(prepStmt);
        //closeConnection(dbConnection);
    }

    public static void closeConnection(Connection dbConnection) {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeStatement(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
