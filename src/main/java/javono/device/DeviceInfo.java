package javono.device;

/**
 * Represents the basic information of a connected Javono ESP32 device.
 */
public class DeviceInfo {

    private final String deviceId;        // Unique device identifier (e.g., UUID or serial number)
    private final String model;           // Device model name or type
    private final String firmwareVersion; // Firmware version running on device
    private final String serialPort;      // Serial port name (e.g., COM3, /dev/ttyUSB0)
    private final String macAddress;      // MAC address of the device's network interface (optional)
    private final String deviceAuthToken;

    public DeviceInfo(String deviceId, String model, String firmwareVersion, String serialPort, String macAddress, String deviceAuthToken) {
        this.deviceId = deviceId;
        this.model = model;
        this.firmwareVersion = firmwareVersion;
        this.serialPort = serialPort;
        this.macAddress = macAddress;
        this.deviceAuthToken = deviceAuthToken;
    }

    // Getters
    public String getDeviceId() {
        return deviceId;
    }

    public String getModel() {
        return model;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getSerialPort() {
        return serialPort;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getDeviceAuthToken() {
        return deviceAuthToken;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", model='" + model + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", serialPort='" + serialPort + '\'' +
                ", macAddress='" + macAddress + '\'' +
                '}';
    }
}
