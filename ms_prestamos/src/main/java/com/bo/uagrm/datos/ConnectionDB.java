package com.bo.uagrm.datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class ConnectionDB {

    private static final String URL;
    private static final String USER;
    private static final String PASS;
    private static final String DRIVER = "org.postgresql.Driver";

    static {
        Properties props = new Properties();
        try (var is = ConnectionDB.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {
        }

        URL = getConfig("DB_URL", props.getProperty("DB_URL"));
        USER = getConfig("DB_USER", props.getProperty("DB_USER"));
        PASS = getConfig("DB_PASS", props.getProperty("DB_PASS"));
    }

    private static String getConfig(String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        return fallback;
    }


    public static Connection getConnection() throws Exception {
        Class.forName(DRIVER);
        return DriverManager.getConnection(URL, USER, PASS);
    }


}
