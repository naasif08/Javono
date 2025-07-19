package javono.installer;

import javono.detector.OS;
import javono.detector.PathDetector;
import javono.logger.JavonoLogger;
import javono.utils.FileDownloader;
import javono.utils.ZipExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EspIdfInstaller {

    public static boolean isInstalledForWindows() {
        if (PathDetector.detectIdfPath() != null) {
            Path idfPath = Path.of(PathDetector.detectIdfPath());
            return !Files.exists(idfPath) || !Files.exists(idfPath.resolve("install.bat")); // Windows check
        }
        return true;
    }

    public static void downloadAndInstall() throws IOException {
        String url = "https://dl.espressif.com/github_assets/espressif/esp-idf/releases/download/v5.4.2/esp-idf-v5.4.2.zip";
        Path zipPath = getJavonoFolder().resolve("esp-idf.zip");

        if (!zipPath.getParent().toFile().exists()) Files.createDirectory(zipPath.getParent());

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
            return isInstalledLinux();
        } else if (os.contains("mac")) {
            return isInstalledMac();
        } else {
            JavonoLogger.error("Unsupported OS for ESP-IDF install check.");
            return false;
        }
    }

    private static boolean isInstalledLinux() {
        String idfPath = System.getProperty("user.home") + "/.espressif";
        File idfFolder = new File(idfPath);
        File idfPy = new File(idfPath + "/python_env/bin/idf.py");

        return idfFolder.exists() && idfPy.exists() && idfPy.canExecute();
    }

    private static boolean isInstalledMac() {
        // Assuming default install path for macOS
        String idfPath = System.getProperty("user.home") + "/.espressif";
        File idfFolder = new File(idfPath);
        File idfPy = new File(idfPath + "/python_env/bin/idf.py");

        return idfFolder.exists() && idfPy.exists() && idfPy.canExecute();
    }

    // Optionally verify by actually running idf.py --version
    public static boolean verifyByCommand() {
        try {
            Process process = new ProcessBuilder("idf.py", "--version")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
