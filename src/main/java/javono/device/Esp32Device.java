package javono.device;

import javono.serial.JavonoSerialThreaded;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a Javono-managed ESP32 device.
 * Handles connection (serial/OTA), firmware updates, and command sending.
 */
class Esp32Device {

    private final DeviceInfo deviceInfo;

    private JavonoSerialThreaded serialConnection;
    private OTAUploader otaUploader;

    public Esp32Device(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public void connectSerial() throws IOException {
        if (serialConnection != null && serialConnection.isConnected()) {
            return;
        }
        serialConnection = new JavonoSerialThreaded(deviceInfo.getSerialPort());
        serialConnection.open();
    }

    public void disconnectSerial() {
        if (serialConnection != null) {
            serialConnection.close();
            serialConnection = null;
        }
    }

    public Optional<String> sendSerialCommand(String command) throws IOException {
        if (serialConnection == null || !serialConnection.isConnected()) {
            throw new IOException("Serial connection is not open");
        }
        serialConnection.write(command);
        return serialConnection.readResponse(); // depends on your JavonoSerialThreaded design
    }

    public void uploadFirmwareOTA(Path firmwareBinPath) throws IOException {
        if (otaUploader == null) {
            otaUploader = new OTAUploader(deviceInfo);
        }
        otaUploader.upload(firmwareBinPath);
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
}
