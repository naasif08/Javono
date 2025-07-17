package javono.serial;


import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Threaded serial communication manager for ESP32 device.
 * Uses a background thread to read data and a write queue for outgoing commands.
 */
public class JavonoSerialThreaded {

    private final String portName;
    private SerialPort comPort;
    private InputStream in;
    private OutputStream out;

    private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();
    private final StringBuilder responseBuffer = new StringBuilder();

    private Thread ioThread;
    private volatile boolean running = false;

    public JavonoSerialThreaded(String portName) {
        this.portName = portName;
    }

    /**
     * Opens the serial connection and starts the IO thread.
     */
    public void open() throws IOException {
        comPort = SerialPort.getCommPort(portName);
        comPort.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (!comPort.openPort()) {
            throw new IOException("Failed to open serial port: " + portName);
        }

        in = comPort.getInputStream();
        out = comPort.getOutputStream();

        running = true;
        ioThread = new Thread(this::runIO, "JavonoSerialThread");
        ioThread.start();
    }

    /**
     * Sends a command to the device.
     *
     * @param command command string
     */
    public void write(String command) {
        writeQueue.offer(command.endsWith("\n") ? command : command + "\n");
    }

    /**
     * Reads the latest line response from the device.
     * Clears buffer after reading.
     *
     * @return Optional containing response, or empty if nothing
     */
    public Optional<String> readResponse() {
        synchronized (responseBuffer) {
            if (responseBuffer.length() == 0) return Optional.empty();
            String result = responseBuffer.toString().trim();
            responseBuffer.setLength(0);
            return Optional.of(result);
        }
    }

    /**
     * Background I/O thread that reads serial data and writes from queue.
     */
    private void runIO() {
        byte[] readBuffer = new byte[1024];
        while (running && comPort != null && comPort.isOpen()) {
            try {
                // Handle incoming data
                while (in.available() > 0) {
                    int numRead = in.read(readBuffer);
                    if (numRead > 0) {
                        String data = new String(readBuffer, 0, numRead, StandardCharsets.UTF_8);
                        synchronized (responseBuffer) {
                            responseBuffer.append(data);
                        }
                    }
                }

                // Handle outgoing data
                String command = writeQueue.poll();
                if (command != null && out != null) {
                    out.write(command.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

                Thread.sleep(20); // avoid busy waiting
            } catch (Exception e) {
                System.err.println("Serial I/O error: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Closes the serial connection and stops background thread.
     */
    public void close() {
        running = false;
        if (ioThread != null) {
            try {
                ioThread.join(500);
            } catch (InterruptedException ignored) {}
        }

        if (in != null) try { in.close(); } catch (IOException ignored) {}
        if (out != null) try { out.close(); } catch (IOException ignored) {}
        if (comPort != null) comPort.closePort();

        in = null;
        out = null;
        comPort = null;
    }

    /**
     * Checks if the serial connection is alive.
     */
    public boolean isConnected() {
        return comPort != null && comPort.isOpen();
    }
}
