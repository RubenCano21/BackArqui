package com.bo.uagrm.config;

import java.util.Properties;

/**
 * Centraliza la lectura de configuración desde application.properties
 * o variables de entorno (misma prioridad que ConnectionDB).
 */
public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (var is = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return env;
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return props.getProperty(key, "");
    }

    public static int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(get(key)); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}