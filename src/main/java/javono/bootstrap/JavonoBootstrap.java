package javono.bootstrap;


import javono.detector.DetectorFacade;
import javono.detector.OS;
import javono.installer.*;

import javono.logger.LoggerFacade;
import javono.utils.UtilsFacade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;


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

    public static void installCli() {
        boolean isWindows = OS.detect().isWindows();
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path binDir = userHome.resolve(".javono/bin");

        try {
            Files.createDirectories(binDir);

            LoggerFacade.getInstance().info("Installing Javono CLI...");
            String jarUrl = "https://github.com/naasif08/JavonoProject/releases/download/Javono-CLI-v1.0/Javono.jar";
            Path jarPath = binDir.resolve("javono.jar");
            try (InputStream in = new java.net.URL(jarUrl).openStream()) {
                Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String scriptResource = isWindows ? "/scripts/javono.bat" : "/scripts/javono";
            Path scriptPath = binDir.resolve(isWindows ? "javono.bat" : "javono");
            try (InputStream scriptStream = JavonoBootstrap.class.getResourceAsStream(scriptResource)) {
                if (scriptStream == null) throw new RuntimeException("Cannot find script: " + scriptResource);
                Files.copy(scriptStream, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!isWindows) {
                scriptPath.toFile().setExecutable(true);
            }

            if (isWindows) {
                try {
                    new ProcessBuilder("cmd", "/c", "setx PATH \"%PATH%;" + binDir.toString() + "\"")
                            .inheritIO()
                            .start()
                            .waitFor();
                    LoggerFacade.getInstance().info("Javono CLI installed at: " + binDir);
                    LoggerFacade.getInstance().info("Restart terminal to use `javono`.");
                } catch (Exception e) {
                    LoggerFacade.getInstance().error("Could not update PATH automatically.");
                    LoggerFacade.getInstance().error("Add " + binDir + " to your PATH manually.");
                }
            } else {

                String shell = System.getenv("SHELL");
                Path rcFile = shell != null && shell.contains("zsh") ? userHome.resolve(".zshrc")
                        : shell != null && shell.contains("fish") ? userHome.resolve(".config/fish/config.fish")
                        : userHome.resolve(".bashrc");

                String exportLine = shell != null && shell.contains("fish")
                        ? "set -gx PATH " + binDir.toString() + " $PATH"
                        : "export PATH=\"" + binDir.toString() + ":$PATH\"";

                Files.write(rcFile, Arrays.asList("\n# Added by Javono", exportLine),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                LoggerFacade.getInstance().success("Javono CLI installed at: " + binDir);
                LoggerFacade.getInstance().success("Added PATH update to " + rcFile + ". Please restart your terminal.");
            }

            LoggerFacade.getInstance().info("You can now run `javono help` to see available commands.");

        } catch (IOException e) {
            throw new RuntimeException("Failed to install Javono CLI", e);
        }
    }
}