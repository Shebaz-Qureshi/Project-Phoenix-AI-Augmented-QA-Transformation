package com.phoenix.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ============================================================================
 * ConfigReader  –  Centralised Configuration Access
 * ============================================================================
 * Loads config.properties from the classpath.
 * System properties (e.g., -Dbrowser=firefox) override file values — this
 * is the mechanism used to inject CI environment variables into the suite.
 *
 * Usage:
 *   String browser = ConfigReader.get("browser");
 *   int timeout    = ConfigReader.getInt("explicitWait");
 *   boolean headless = ConfigReader.getBool("headless");
 * ============================================================================
 */
public class ConfigReader {

    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    private static final Properties props = new Properties();

    static {
        try (InputStream in = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (in == null) {
                throw new RuntimeException("config.properties not found on classpath");
            }
            props.load(in);
            log.info("config.properties loaded successfully");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private ConfigReader() { /* utility class */ }

    /**
     * Returns the value for the given key.
     * System property takes precedence over config file.
     */
    public static String get(String key) {
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            log.debug("Config override via system property: {} = {}", key, sysProp);
            return sysProp;
        }
        String value = props.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing config key: '" + key + "'");
        }
        return value.trim();
    }

    /** Returns an int config value. */
    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    /** Returns a boolean config value (accepts "true" / "false"). */
    public static boolean getBool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /** Returns a value with a fallback default if the key is missing. */
    public static String getOrDefault(String key, String defaultValue) {
        try {
            return get(key);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }
}
