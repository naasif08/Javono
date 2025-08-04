package javono.device;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Handles OTA firmware upload to the Spring Boot backend for ESP32 devices.
 */
class OTAUploader {

    private static final String UPLOAD_URL = "http://your-backend-host/api/firmware/upload";

    private final DeviceInfo deviceInfo;

    public OTAUploader(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    /**
     * Uploads firmware binary to the backend OTA endpoint.
     *
     * @param firmwareBinPath path to firmware .bin file
     * @throws IOException if upload fails
     */
    public void upload(Path firmwareBinPath) throws IOException {
        String boundary = UUID.randomUUID().toString();
        String CRLF = "\r\n";

        HttpURLConnection connection = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = connection.getOutputStream()) {
            // Device ID
            writeFormField(output, "deviceId", deviceInfo.getDeviceId(), boundary);

            // Auth token (if any)
            if (deviceInfo.getDeviceAuthToken() != null) {
                writeFormField(output, "authToken", deviceInfo.getDeviceAuthToken(), boundary);
            }

            // Firmware file
            byte[] firmwareBytes = Files.readAllBytes(firmwareBinPath);
            output.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Disposition: form-data; name=\"firmware\"; filename=\"firmware.bin\"" + CRLF).getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Type: application/octet-stream" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
            output.write(firmwareBytes);
            output.write(CRLF.getBytes(StandardCharsets.UTF_8));

            // End boundary
            output.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
            output.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("OTA upload failed with status: " + responseCode);
        }
    }

    /**
     * Helper to write a form field.
     */
    private void writeFormField(OutputStream output, String name, String value, String boundary) throws IOException {
        String CRLF = "\r\n";
        output.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: text/plain; charset=UTF-8" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write(CRLF.getBytes(StandardCharsets.UTF_8));
    }
}
