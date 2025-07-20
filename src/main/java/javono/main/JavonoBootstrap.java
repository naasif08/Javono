package javono.main;

import javono.detector.OS;
import javono.detector.PathDetector;
import javono.installer.*;
import javono.logger.JavonoLogger;
import javono.utils.PythonEnvChecker;

import java.io.IOException;

public class JavonoBootstrap {
    public static void runFirstTimeSetupLocal() throws IOException, InterruptedException {
        OS currentOS = OS.detect();
        if (!EspIdfInstaller.isIdfInstalled()) {
            JavonoLogger.info("Setting up Environment for javono...");
            if (currentOS == OS.WINDOWS) {
                EspIdfInstaller.downloadAndInstall();
                PythonInstaller.ensureMinicondaInstalled();
                GitInstaller.ensureGitInstalled();
                JavonoLogger.info("Installing ESP dependencies.");
                PythonEnvChecker.warnAndInstallIfMissing();
                ESPInstaller.runInstallScript();
                if (!EspIdfInstaller.isInstalledForWindows()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                }
            } else if (currentOS == OS.LINUX) {
                if (!EspIdfInstaller.isInstalledForUnix()) {
                    // Use terminal-based installer for Linux/macOS
                    EspIdfInstallerUnix.installForLinux();
                }
                // After terminal install finishes, you might want to verify installation again
                if (!EspIdfInstaller.isInstalledForLinux()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                }
            } else if (currentOS == OS.MACOS) {
                EspIdfInstallerUnix.installForMacOS();
                if (!EspIdfInstaller.isInstalledForMac()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
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

}
