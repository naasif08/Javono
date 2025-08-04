package javono.bootstrap;


import javono.detector.DetectorFacade;
import javono.detector.OS;
import javono.installer.*;

import javono.logger.LoggerFacade;
import javono.utils.UtilsFacade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class JavonoBootstrap {
    public static void setEnvironmentForTheFirstTime() {
        OS currentOS = OS.detect();
        if (!InstallerFacade.getInstance().isEspIdfInstalled()) {
            LoggerFacade.getInstance().info("Setting up Environment for Javono...");
            if (currentOS == OS.WINDOWS) {
                if (!InstallerFacade.getInstance().isEspIdfInstalledForWindows()) {
                    try {
                        InstallerFacade.getInstance().downloadAndInstallEspIdf();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    UtilsFacade.getInstance().warnAndInstallIfMissing();
                    InstallerFacade.getInstance().ensureGitInstalled();
                    LoggerFacade.getInstance().info("Installing ESP dependencies.");
                    try {
                        InstallerFacade.getInstance().runInstallScript();
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    completeInstallationFlag();
                }
                if (!InstallerFacade.getInstance().isEspIdfInstalledForWindows()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                } else {
                    LoggerFacade.getInstance().success("Esp IDF already installed.");
                }

            } else if (currentOS == OS.LINUX) {
                if (!InstallerFacade.getInstance().isEspIdfInstalledForLinux()) {
                    // Use terminal-based installer for Linux/macOS
                    InstallerFacade.getInstance().installEspIdfForLinux();
                    completeInstallationFlag();
                }
                // After terminal install finishes, you might want to verify installation again
                if (!InstallerFacade.getInstance().isEspIdfInstalledForLinux()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                } else {
                    LoggerFacade.getInstance().success("Esp IDF already installed.");
                }

            } else if (currentOS == OS.MACOS) {
                if (!InstallerFacade.getInstance().isEspIdfInstalledForMac()) {
                    // Use terminal-based installer for Linux/macOS
                    InstallerFacade.getInstance().installEspIdfForMacOS();
                    completeInstallationFlag();
                }
                if (!InstallerFacade.getInstance().isEspIdfInstalledForMac()) {
                    throw new RuntimeException("ESP-IDF installation failed or incomplete.");
                } else {
                    LoggerFacade.getInstance().success("Esp IDF already installed.");
                }
            } else {
                throw new UnsupportedOperationException("Unsupported OS: " + currentOS);
            }
        } else {
            LoggerFacade.getInstance().success("Already Javono Environment installed...");
        }
        LoggerFacade.getInstance().success("All Dependencies are installed successfully!");
        InstallerFacade.getInstance().installEsp32DeviceDriver();
        DetectorFacade.getInstance().printDetectedPaths();
    }

    private static void completeInstallationFlag() {
        Path flagFile = Paths.get(InstallerFacade.getInstance().getJavonoFolder().toString(), "installcomplete.txt");
        try {
            Files.writeString(flagFile, "Installation completed successfully.\n");
            LoggerFacade.getInstance().info("Created installcomplete.txt at: " + flagFile);
        } catch (IOException e) {
            LoggerFacade.getInstance().error("Failed to create installcomplete.txt: " + e.getMessage());
        }
    }

}
