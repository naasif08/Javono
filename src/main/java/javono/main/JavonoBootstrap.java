package javono.main;


import javono.detector.OS;
import javono.detector.PathDetector;
import javono.installer.*;
import javono.logger.JavonoLogger;
import javono.utils.PythonEnvChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class JavonoBootstrap {
    public static void runFirstTimeSetupLocal() throws IOException, InterruptedException {
        OS currentOS = OS.detect();
        if (!EspIdfInstaller.isIdfInstalled()) {
            JavonoLogger.info("Setting up Environment for Javono...");
            if (currentOS == OS.WINDOWS) {
                if (!EspIdfInstaller.isInstalledForWindows()) {
                    EspIdfInstaller.downloadAndInstall();
                    PythonEnvChecker.warnAndInstallIfMissing();
                    GitInstaller.ensureGitInstalled();
                    JavonoLogger.info("Installing ESP dependencies.");
                    ESPInstaller.runInstallScript();
                    completeInstallationFlag();
                }
                if (!EspIdfInstaller.isInstalledForWindows()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                } else {
                    JavonoLogger.success("Esp IDF already installed.");
                }

            } else if (currentOS == OS.LINUX) {
                if (!EspIdfInstaller.isInstalledForLinux()) {
                    // Use terminal-based installer for Linux/macOS
                    EspIdfInstallerUnix.installForLinux();
                    completeInstallationFlag();
                }
                // After terminal install finishes, you might want to verify installation again
                if (!EspIdfInstaller.isInstalledForLinux()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                } else {
                    JavonoLogger.success("Esp IDF already installed.");
                }

            } else if (currentOS == OS.MACOS) {
                if (!EspIdfInstaller.isInstalledForMac()) {
                    // Use terminal-based installer for Linux/macOS
                    EspIdfInstallerUnix.installForMacOS();
                    completeInstallationFlag();
                }
                if (!EspIdfInstaller.isInstalledForMac()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                } else {
                    JavonoLogger.success("Esp IDF already installed.");
                }
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + currentOS);
            }
        } else {
            JavonoLogger.success("Already Javono Environment installed...");
        }
        JavonoLogger.success("All Dependencies are installed successfully!");
        CH340Installer.installDriver();
        PathDetector.printDetectedPaths();
    }

    private static void completeInstallationFlag() {
        Path flagFile = Paths.get(EspIdfInstaller.getJavonoFolder().toString(), "installcomplete.txt");
        try {
            Files.writeString(flagFile, "Installation completed successfully.\n");
            JavonoLogger.info("Created installcomplete.txt at: " + flagFile);
        } catch (IOException e) {
            JavonoLogger.error("Failed to create installcomplete.txt: " + e.getMessage());
        }
    }

}
