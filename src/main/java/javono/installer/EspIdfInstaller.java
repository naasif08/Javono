package javono.installer;

import javono.detector.OS;
import javono.detector.PathDetector;
import javono.logger.JavonoLogger;
import javono.main.JavonoBootstrap;
import javono.utils.FileDownloader;
import javono.utils.ZipExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static javono.detector.PathDetector.VERSION;

public class EspIdfInstaller {

    public static boolean isIdfInstalled() {
        OS os = OS.detect();
        if (os == OS.WINDOWS) {
            return isInstalledForWindows();
        } else if (os == OS.LINUX) {
            return isInstalledForLinux();
        } else if (os == OS.MACOS) {
            return isInstalledForMac();
        }
        return false;
    }

    public static boolean isInstalledForWindows() {
        String path = Paths.get("C:", "Javono").toString();
        File javonoFolder = new File(path);
        File installFile = new File(path + "/installcomplete.txt");
        return javonoFolder.exists() && installFile.exists() && installFile.canExecute();
    }

    public static void downloadAndInstall() throws IOException {
        String url = "https://dl.espressif.com/github_assets/espressif/esp-idf/releases/download/v5.4.2/esp-idf-v5.4.2.zip";
        Path zipPath = getJavonoFolder().resolve("esp-idf.zip");
        Path completeFlag = Path.of(getJavonoFolder().toString() + "//esp-idf-" + VERSION).resolve("complete.txt");
        File file = completeFlag.toFile();

        if (!zipPath.getParent().toFile().exists()) Files.createDirectory(zipPath.getParent());
        if (file.exists()) return;

        Path targetDir = zipPath.getParent();

        try {
            JavonoLogger.info("Downloading ESP-IDF...");
            FileDownloader.downloadWithResume(url, zipPath);

            JavonoLogger.info("Extracting...");
            ZipExtractor.extract(zipPath, targetDir); // extract into /Javono/

            Files.delete(zipPath); // optional cleanup
            JavonoLogger.success("ESP-IDF installed at: " + targetDir);
        } catch (IOException e) {
            throw new RuntimeException("❌ Installation failed: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Path flagFile = Paths.get(EspIdfInstaller.getJavonoFolder().toString(), "esp-idf-v5.4.2", "complete.txt");
        try {
            Files.writeString(flagFile, "Extraction completed successfully.\n");
        } catch (IOException e) {
            JavonoLogger.error("Failed to create complete.txt: " + e.getMessage());
        }
    }

    public static Path getJavonoFolder() {
        OS os = getOSType();
        return switch (os) {
            case WINDOWS -> Paths.get("C:", "Javono");
            case MACOS, LINUX -> Paths.get(System.getProperty("user.home"), "Javono");
            default -> throw new UnsupportedOperationException("Unsupported OS for Javono setup.");
        };
    }

    public static OS getOSType() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return OS.WINDOWS;
        if (os.contains("mac")) return OS.MACOS;
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return OS.LINUX;
        return OS.UNKNOWN;
    }

    public static boolean isInstalledForUnix() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("nux")) {
            return isInstalledForLinux();
        } else if (os.contains("mac")) {
            return isInstalledForMac();
        } else {
            JavonoLogger.error("Unsupported OS for ESP-IDF install check.");
            return false;
        }
    }

    public static boolean isInstalledForLinux() {
        String path = System.getProperty("user.home") + "/Javono";
        File javonoFolder = new File(path);
        File installFile = new File(path + "/installcomplete.txt");
        return javonoFolder.exists() && installFile.exists() && installFile.canExecute();
    }

    public static boolean isInstalledForMac() {
        // Assuming default install path for macOS
        String path = System.getProperty("user.home") + "/Javono";
        File javonoFolder = new File(path);
        File installFile = new File(path + "/installcomplete.txt");
        return javonoFolder.exists() && installFile.exists() && installFile.canExecute();
    }

    // Optionally verify by actually running idf.py --version
    public static boolean verifyByCommand() {
        try {
            Process process = new ProcessBuilder("idf.py", "--version").redirectErrorStream(true).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
