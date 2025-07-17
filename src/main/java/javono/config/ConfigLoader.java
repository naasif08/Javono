package javono.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * ConfigLoader loads Javono project configuration from a properties file.
 * Defaults to loading from ~/.javono/javono.properties but can load from any path.
 */
public class ConfigLoader {

    private final Properties properties = new Properties();

    /**
     * Loads config from the default location (~/.javono/javono.properties).
     *
     * @throws IOException if loading fails
     */
    public void loadDefault() throws IOException {
        Path defaultPath = Path.of(System.getProperty("user.home"), ".Javono", "javono.properties");
        load(defaultPath);
    }

    /**
     * Loads config from a specified properties file path.
     *
     * @param path Path to the properties file
     * @throws IOException if loading fails
     */
    public void load(Path path) throws IOException {
        if (Files.notExists(path)) {
            throw new IOException("Config file not found: " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }
    }

    /**
     * Get a property value by key or return defaultValue if key not present.
     *
     * @param key property key
     * @param defaultValue fallback value
     * @return property value or defaultValue
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get a required property value by key.
     *
     * @param key property key
     * @return property value
     * @throws IllegalStateException if key not found
     */
    public String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Required config property missing: " + key);
        }
        return value;
    }

    /**
     * Check if config contains a key.
     *
     * @param key property key
     * @return true if present
     */
    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * Returns all loaded properties.
     *
     * @return Properties object
     */
    public Properties getAll() {
        return properties;
    }
}
