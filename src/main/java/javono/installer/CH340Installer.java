package javono.installer;

import javono.detector.OS;
import javono.logger.JavonoLogger;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;

public class CH340Installer {

    public static void installDriver() {
        if (isDriverInstalled()) {
            JavonoLogger.success("CH340 driver is already installed.");
            return;
        }

        OS os = OS.detect();

        JavonoLogger.info("CH340 driver not found. Installing now on " + os + "...");

        switch (os) {
            case WINDOWS:
                installWindows();
                break;
            case MACOS:
                installMac();
                break;
            case LINUX:
                installLinux();
                break;
            default:
                JavonoLogger.error("Unsupported OS: " + os);
        }
    }

    public static boolean isDriverInstalled() {
        OS os = OS.detect();

        return switch (os) {
            case WINDOWS -> isDriverInstalledOnWindows();
            case MACOS -> isDriverInstalledOnMac();
            case LINUX -> isDriverInstalledOnLinux();
            default -> false;
        };
    }

    private static boolean isDriverInstalledOnWindows() {
        try {
            Process process = Runtime.getRuntime().exec("driverquery /v /fo csv");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase(Locale.ROOT).contains("ch34")) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            JavonoLogger.error("⚠️ Unable to check driver on Windows: " + e.getMessage());
        }
        return false;
    }

    private static boolean isDriverInstalledOnMac() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"kextstat"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String l = line.toLowerCase(Locale.ROOT);
                    if (l.contains("usbserial") || l.contains("wch") || l.contains("ch34")) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            JavonoLogger.error("⚠️ Unable to check kext on macOS: " + e.getMessage());
        }
        return false;
    }

    private static boolean isDriverInstalledOnLinux() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"lsmod"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ch341")) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            JavonoLogger.error("⚠️ Unable to check kernel module on Linux: " + e.getMessage());
        }
        return false;
    }

    private static void installWindows() {
        try (InputStream in = CH340Installer.class.getResourceAsStream("/drivers/windows/CH341SER.EXE")) {
            if (in == null) {
                JavonoLogger.error("CH341SER.EXE not found in JAR.");
                return;
            }
            File tempExe = File.createTempFile("ch340-installer", ".exe");
            Files.copy(in, tempExe.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tempExe.setExecutable(true);

            JavonoLogger.info("Extracted to: " + tempExe.getAbsolutePath());

            // Launch installer (normal)
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", tempExe.getAbsolutePath()});
            JavonoLogger.info("CH340 installer launched on Windows.");

        } catch (IOException e) {
            JavonoLogger.error("Windows install failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void installMac() {
        try (InputStream in = CH340Installer.class.getResourceAsStream("/drivers/macos/CH34x_Install.pkg")) {
            if (in == null) {
                JavonoLogger.error("CH34x_Install.pkg not found in JAR.");
                return;
            }
            File tempPkg = File.createTempFile("ch340-mac", ".pkg");
            Files.copy(in, tempPkg.toPath(), StandardCopyOption.REPLACE_EXISTING);

            JavonoLogger.info("Extracted to: " + tempPkg.getAbsolutePath());

            // Open installer
            Runtime.getRuntime().exec(new String[]{"open", tempPkg.getAbsolutePath()});
            JavonoLogger.success("CH340 installer launched on macOS.");

        } catch (IOException e) {
            JavonoLogger.error("macOS install failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void installLinux() {
        JavonoLogger.info("On Linux, CH340 drivers are usually built-in.");
        JavonoLogger.info("See drivers/ch340/linux/README.txt for manual setup if needed.");

        // Optionally extract README.txt to current dir for user convenience
        try (InputStream in = CH340Installer.class.getResourceAsStream("/drivers/linux/README.txt")) {
            if (in != null) {
                File doc = new File("CH340-Linux-README.txt");
                Files.copy(in, doc.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("📁 Linux guide extracted to: " + doc.getAbsolutePath());
            }
        } catch (IOException e) {
            JavonoLogger.error("Failed to extract Linux README: " + e.getMessage());
        }
    }
}
