package org.wso2.scim.metadata.population.tool.db;

import org.wso2.scim.metadata.population.tool.IdentityException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Initiate Database connection.
 */
public class DBConnection {

    /**
     * Create database connection if closed. Else return existing connection.
     * @param url url of database.
     * @param username username of database.
     * @param password password of database.
     * @return database connection.
     */
    public static Connection getConnection(String url, String username, String password) throws IdentityException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IdentityException("Unable to connect to database.", e);
        }
        return connection;
    }

    /**
     * Load JDBC driver.
     * @param driverLoacation location of JAR file.
     * @param jdbcConnectionClass Connection classs name.
     */
    public static void loadDBDriver(String driverLoacation, String jdbcConnectionClass) throws IdentityException {
        File file = new File(driverLoacation);
        URL url = null;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IdentityException("Unable to open url.", e);
        }
        URLClassLoader ucl = new URLClassLoader(new URL[] {url});
        Driver driver = null;
        try {
            driver = (Driver) Class.forName(jdbcConnectionClass, true, ucl).newInstance();
        } catch (InstantiationException e) {
            throw new IdentityException("Unable to load the JDBC driver.", e);
        } catch (IllegalAccessException e) {
            throw new IdentityException("Unable to load the JDBC driver.", e);
        } catch (ClassNotFoundException e) {
            throw new IdentityException("Unable to load the JDBC driver.", e);
        }
        try {
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (SQLException e) {
            throw new IdentityException("Unable to register the JDBC driver.", e);
        }
    }
}
