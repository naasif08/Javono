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

        if (EspIdfInstaller.isInstalledForWindows()) {
            JavonoLogger.info("Setting up Environment for javono...");

            if (currentOS == OS.WINDOWS) {
                EspIdfInstaller.downloadAndInstall(); // ZIP download + extract on Windows
            } else if (currentOS == OS.LINUX) {
                if (!EspIdfInstaller.isInstalledForUnix()) {
                    // Use terminal-based installer for Linux/macOS
                    EspIdfInstallerUnix.installForLinux();
                }
                // After terminal install finishes, you might want to verify installation again
                if (!EspIdfInstaller.isInstalledForWindows() || !EspIdfInstaller.isInstalledForUnix()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                }
            } else if (currentOS == OS.MACOS) {
                EspIdfInstallerUnix.installForMacOS();
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + currentOS);
            }
        } else {
            JavonoLogger.success("Already Javono Environment installed...");
        }

        if (OS.detect().isWindows()) {
            try {
                PythonInstaller.ensureMinicondaInstalled();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            GitInstaller.ensureGitInstalled();
            JavonoLogger.info("Installing ESP dependencies.");
            try {
                PythonEnvChecker.warnAndInstallIfMissing();
                ESPInstaller.runInstallScript();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        JavonoLogger.success("All Dependencies are installed successfully!");
        CH340Installer.installDriver();
        PathDetector.printDetectedPaths();
    }

}
