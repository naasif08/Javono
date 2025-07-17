package javono.config;

import java.nio.file.Path;

/**
 * JavonoConfig provides typed accessors to Javono configuration properties.
 * It wraps ConfigLoader and exposes domain-specific getters.
 */
public class Esp32Config {

    private final ConfigLoader configLoader;

    public Esp32Config(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    /**
     * Gets the ESP-IDF path.
     *
     * @return Path to ESP-IDF directory or null if not set
     */
    public Path getIdfPath() {
        String pathStr = configLoader.get("idf.path", null);
        return (pathStr == null || pathStr.isBlank()) ? null : Path.of(pathStr);
    }

    /**
     * Gets the GitHub access token for ESP-IDF repo (optional).
     *
     * @return GitHub token or null if not set
     */
    public String getIdfAccessToken() {
        return configLoader.get("idf.github.token", null);
    }

    /**
     * Gets the URL for ESP-IDF GitHub repo.
     *
     * @return URL string or default repo URL
     */
    public String getIdfRepoUrl() {
        return configLoader.get("idf.github.repo", "https://github.com/espressif/esp-idf.git");
    }

    /**
     * Gets the GitHub access token for user firmware repo.
     *
     * @return token or null
     */
    public String getFirmwareAccessToken() {
        return configLoader.get("firmware.github.token", null);
    }

    /**
     * Gets the URL for user firmware GitHub repo.
     *
     * @return URL string or null if not set
     */
    public String getFirmwareRepoUrl() {
        return configLoader.get("firmware.github.repo", null);
    }

    /**
     * Gets whether automatic downloading of ESP-IDF is allowed.
     *
     * @return true if allowed, false otherwise
     */
    public boolean isAllowDownload() {
        return Boolean.parseBoolean(configLoader.get("idf.allowDownload", "false"));
    }

    /**
     * Gets the device authentication token.
     *
     * @return device token string or null
     */
    public String getDeviceAuthToken() {
        return configLoader.get("device.auth.token", null);
    }

    // Add more typed getters as needed for your configuration keys...

}

