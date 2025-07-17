package javono.installer;

import javono.detector.PathDetector;
import javono.logger.JavonoLogger;

import java.io.IOException;

public class JavonoBootstrap {
    public static void runFirstTimeSetupLocal() throws IOException {
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
            ESPInstaller.runInstallScript();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        JavonoLogger.success("All Dependencies are installed successfully!");
        CH340Installer.installDriver();
        PathDetector.printDetectedPaths();
    }

}
