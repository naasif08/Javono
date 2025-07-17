package javono.config;

import java.nio.file.Path;

/**
 * Esp32Properties is a plain data holder for Javono configuration values.
 * It can be used to encapsulate config data in a single object.
 */
public class Esp32Properties {

    private Path idfPath;
    private String idfAccessToken;
    private String idfRepoUrl;
    private boolean allowDownload;

    private String firmwareRepoUrl;
    private String firmwareAccessToken;

    private String deviceAuthToken;

    // Constructors
    public Esp32Properties() {
    }

    public Esp32Properties(Path idfPath, String idfAccessToken, String idfRepoUrl, boolean allowDownload,
                           String firmwareRepoUrl, String firmwareAccessToken, String deviceAuthToken) {
        this.idfPath = idfPath;
        this.idfAccessToken = idfAccessToken;
        this.idfRepoUrl = idfRepoUrl;
        this.allowDownload = allowDownload;
        this.firmwareRepoUrl = firmwareRepoUrl;
        this.firmwareAccessToken = firmwareAccessToken;
        this.deviceAuthToken = deviceAuthToken;
    }

    // Getters and Setters
    public Path getIdfPath() {
        return idfPath;
    }

    public void setIdfPath(Path idfPath) {
        this.idfPath = idfPath;
    }

    public String getIdfAccessToken() {
        return idfAccessToken;
    }

    public void setIdfAccessToken(String idfAccessToken) {
        this.idfAccessToken = idfAccessToken;
    }

    public String getIdfRepoUrl() {
        return idfRepoUrl;
    }

    public void setIdfRepoUrl(String idfRepoUrl) {
        this.idfRepoUrl = idfRepoUrl;
    }

    public boolean isAllowDownload() {
        return allowDownload;
    }

    public void setAllowDownload(boolean allowDownload) {
        this.allowDownload = allowDownload;
    }

    public String getFirmwareRepoUrl() {
        return firmwareRepoUrl;
    }

    public void setFirmwareRepoUrl(String firmwareRepoUrl) {
        this.firmwareRepoUrl = firmwareRepoUrl;
    }

    public String getFirmwareAccessToken() {
        return firmwareAccessToken;
    }

    public void setFirmwareAccessToken(String firmwareAccessToken) {
        this.firmwareAccessToken = firmwareAccessToken;
    }

    public String getDeviceAuthToken() {
        return deviceAuthToken;
    }

    public void setDeviceAuthToken(String deviceAuthToken) {
        this.deviceAuthToken = deviceAuthToken;
    }

    @Override
    public String toString() {
        return "Esp32Properties{" +
                "idfPath=" + idfPath +
                ", idfAccessToken='" + idfAccessToken + '\'' +
                ", idfRepoUrl='" + idfRepoUrl + '\'' +
                ", allowDownload=" + allowDownload +
                ", firmwareRepoUrl='" + firmwareRepoUrl + '\'' +
                ", firmwareAccessToken='" + firmwareAccessToken + '\'' +
                ", deviceAuthToken='" + deviceAuthToken + '\'' +
                '}';
    }
}
