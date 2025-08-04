package javono.installer;

import javono.detector.OS;

import javono.logger.LoggerFacade;
import javono.utils.UtilsFacade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static javono.detector.DetectorFacade.VERSION;

class EspIdfInstaller {

    public boolean isIdfInstalled() {
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


    public void downloadAndInstall() throws IOException {
        String url = "https://dl.espressif.com/github_assets/espressif/esp-idf/releases/download/" + VERSION + "/esp-idf-" + VERSION + ".zip";
        Path zipPath = getJavonoFolder().resolve("esp-idf.zip");
        Path completeFlag = Path.of(getJavonoFolder().toString() + "//esp-idf-" + VERSION).resolve("complete.txt");
        File file = completeFlag.toFile();

        if (!zipPath.getParent().toFile().exists()) Files.createDirectory(zipPath.getParent());
        if (file.exists()) return;

        Path targetDir = zipPath.getParent();

        try {
            LoggerFacade.getInstance().info("Downloading ESP-IDF...");
            UtilsFacade.getInstance().downloadWithResume(url, zipPath);

            LoggerFacade.getInstance().info("Extracting...");
            UtilsFacade.getInstance().extractZip(zipPath, targetDir);

            Files.delete(zipPath); // optional cleanup
            LoggerFacade.getInstance().success("Extracted at: " + targetDir);
        } catch (IOException e) {
            throw new RuntimeException("❌ Installation failed: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Path flagFile = Paths.get(getJavonoFolder().toString(), "esp-idf-" + VERSION, "complete.txt");
        try {
            Files.writeString(flagFile, "Extraction completed successfully.\n");
        } catch (IOException e) {
            LoggerFacade.getInstance().error("Failed to create complete.txt: " + e.getMessage());
        }
    }

    public Path getJavonoFolder() {
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

    public boolean isInstalledForUnix() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("nux")) {
            return isInstalledForLinux();
        } else if (os.contains("mac")) {
            return isInstalledForMac();
        } else {
            LoggerFacade.getInstance().error("Unsupported OS for ESP-IDF install check.");
            return false;
        }
    }

    public boolean isInstalledForWindows() {
        String path = Paths.get("C:", "Javono").toString();
        File javonoFolder = new File(path);
        File installFile = new File(path + "/installcomplete.txt");
        return javonoFolder.exists() && installFile.exists() && installFile.canRead();
    }

    public boolean isInstalledForLinux() {
        String path = System.getProperty("user.home") + "/Javono";
        File javonoFolder = new File(path);
        File installFile = new File(path + "/installcomplete.txt");
        return javonoFolder.exists() && installFile.exists() && installFile.canRead();
    }

    public boolean isInstalledForMac() {
        // Assuming default install path for macOS
        String path = System.getProperty("user.home") + "/Javono";
        File javonoFolder = new File(path);
        File installFile = new File(path + "/installcomplete.txt");
        return javonoFolder.exists() && installFile.exists() && installFile.canRead();
    }

    // Optionally verify by actually running idf.py --version
    public boolean verifyByCommand() {
        try {
            Process process = new ProcessBuilder("idf.py", "--version").redirectErrorStream(true).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
