package com.example.musiclibrary.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String PROPERTIES_FILE = "/db.properties";

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();

        try (InputStream input = DatabaseConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new SQLException("Could not find db.properties on classpath.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new SQLException("Failed to load db.properties: " + e.getMessage(), e);
        }

        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        if (url == null || user == null) {
            throw new SQLException("db.url or db.user not set in db.properties");
        }

        return DriverManager.getConnection(url, user, password);
    }
}


