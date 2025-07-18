package javono.main;

import javono.detector.OS;
import javono.detector.PathDetector;
import javono.installer.*;
import javono.logger.JavonoLogger;
import javono.utils.PythonEnvChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavonoBootstrap {
    public static void runFirstTimeSetupLocal() throws IOException, InterruptedException {
        if (!EspIdfInstaller.isInstalled()) {
            JavonoLogger.info("Setting up Environment for javono...");
            EspIdfInstaller.downloadAndInstall(); // handles ZIP + extract
        } else {
            JavonoLogger.success("Already Javono Environment installed...");
        }

        try {
            PythonInstaller.ensureMinicondaInstalled(); // handles ZIP + extract
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!GitInstaller.isGitPresent()) {
            try {
                GitInstaller.ensureGitInstalled();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            JavonoLogger.success("Portable Git is already installed");
        }


        JavonoLogger.info("Installing ESP dependencies.");
        try {
            PythonEnvChecker.warnAndInstallIfMissing();
            ESPInstaller.runInstallScript();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        JavonoLogger.success("All Dependencies are installed successfully!");
        if (OS.detect() == OS.LINUX) {
            NinjaInstaller.installNinja(getDefaultInstallPath().toAbsolutePath());
        }
        CH340Installer.installDriver();
        PathDetector.printDetectedPaths();
    }

    private static Path getDefaultInstallPath() {
        OS os = OS.detect();
        if (os == OS.WINDOWS) {
            return Paths.get("C:", "Javono", "esp-idf-v5.4.2");
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono", "esp-idf-v5.4.2");
        }
    }
}
