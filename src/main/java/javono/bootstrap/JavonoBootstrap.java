package javono.bootstrap;


import javono.detector.DetectorFacade;
import javono.detector.OS;
import javono.installer.*;

import javono.logger.LoggerFacade;
import javono.utils.UtilsFacade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


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
            deleteDirectory(binDir);
            Files.createDirectories(binDir);
            LoggerFacade.getInstance().info("Installing Javono CLI...");

            // Download Javono.jar
            String jarUrl = "https://github.com/naasif08/JavonoProject/releases/download/Javono-CLI-v1.0/Javono.jar";
            Path jarPath = binDir.resolve("javono.jar");
            try (InputStream in = new java.net.URL(jarUrl).openStream()) {
                Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (isWindows) {
                // CMD launcher
                Path batPath = binDir.resolve("javono.bat");
                String batContent = "@echo off\njava -cp \"%~dp0\\javono.jar\" javono.cli.JavonoCli %*";
                Files.write(batPath, batContent.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // PowerShell launcher
                Path ps1Path = binDir.resolve("javono.ps1");
                String ps1Content = "java -cp \"$PSScriptRoot\\javono.jar\" javono.cli.JavonoCli @args";
                Files.write(ps1Path, ps1Content.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // Update user PATH using setx (avoids duplicates)
                String currentPath = System.getenv("PATH");
                String binPathStr = binDir.toString();
                if (currentPath == null || !Arrays.asList(currentPath.split(";")).stream()
                        .anyMatch(p -> p.equalsIgnoreCase(binPathStr))) {

                    // Build a unique PATH
                    Set<String> pathEntries = new LinkedHashSet<>(Arrays.asList(currentPath != null ? currentPath.split(";") : new String[0]));
                    pathEntries.removeIf(p -> p.equalsIgnoreCase(binPathStr)); // Remove duplicates
                    pathEntries.add(binPathStr); // Add Javono bin
                    String newPath = String.join(";", pathEntries);

                    new ProcessBuilder("cmd", "/c", "setx PATH \"" + newPath + "\"")
                            .inheritIO()
                            .start()
                            .waitFor();

                    LoggerFacade.getInstance().success("PATH updated. Close and reopen terminal to use `javono`.");
                } else {
                    LoggerFacade.getInstance().info("Javono CLI folder already in PATH.");
                }

            } else {
                // Linux/macOS launcher
                Path scriptPath = binDir.resolve("javono");
                String scriptContent = "#!/bin/sh\njava -cp \"$(dirname \"$0\")/javono.jar\" javono.cli.JavonoCli \"$@\"";
                Files.write(scriptPath, scriptContent.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                scriptPath.toFile().setExecutable(true);

                // Determine shell RC file
                String shell = System.getenv("SHELL");
                Path rcFile = shell != null && shell.contains("zsh") ? userHome.resolve(".zshrc")
                        : shell != null && shell.contains("fish") ? userHome.resolve(".config/fish/config.fish")
                        : userHome.resolve(".bashrc");

                // Prepare export line
                String exportLine = shell != null && shell.contains("fish")
                        ? "set -gx PATH " + binDir.toString() + " $PATH"
                        : "export PATH=\"" + binDir.toString() + ":$PATH\"";

                // Read existing RC file
                List<String> lines = Files.exists(rcFile) ? Files.readAllLines(rcFile) : new ArrayList<>();

                // Append only if not already present
                if (lines.stream().noneMatch(l -> l.contains(binDir.toString()))) {
                    lines.add("\n# Added by Javono");
                    lines.add(exportLine);
                    Files.write(rcFile, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    LoggerFacade.getInstance().success("Added PATH update to " + rcFile + ". Please restart your terminal.");
                } else {
                    LoggerFacade.getInstance().info("Javono CLI folder already in PATH.");
                }
            }

            LoggerFacade.getInstance().success("Javono CLI installed at: " + binDir);
            LoggerFacade.getInstance().info("You can now run `javono help` to see available commands.");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to install Javono CLI", e);
        }
    }

    public static void ensureInstalled() {
        try {
            // Try to locate Espressif toolchain
            DetectorFacade.getInstance().findEspressifGitPath();

            LoggerFacade.getInstance().info(
                    "Javono installation verified"
            );
        } catch (IllegalStateException e) {
            LoggerFacade.getInstance().error(
                    "Javono is not installed!\n" +
                            "Please run: javono init\n" +
                            "This will set up required files in:\n" +
                            "  - Linux/macOS: ~/.javono\n" +
                            "  - Windows: %USERPROFILE%\\.javono"
            );
            System.exit(1); // Hard stop before build
        } catch (Exception e) {
            LoggerFacade.getInstance().error(
                    "Unexpected error while checking installation: " +
                            e.getMessage()
            );
            System.exit(1);
        }
    }

    public static void uninstallJavono() {
        String userHome = System.getProperty("user.home");
        File userJavono = new File(userHome, ".javono");
        deleteRecursively(userJavono, "user .javono folder");

        // OS-specific system folder
        OS os = OS.detect();
        File systemJavono = null;
        if (os.isWindows()) {
            systemJavono = new File("C:\\Javono");
        } else if (os.isLinux() || os.isMac() || os.isUnixLike()) {
            systemJavono = new File("/opt/Javono");
        }

        if (systemJavono != null && systemJavono.exists()) {
            LoggerFacade.getInstance().info("Detected system Javono folder at: " + systemJavono.getAbsolutePath());
            LoggerFacade.getInstance().info("Do you want to delete this folder? (yes/no)");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("yes") || input.equals("y")) {
                deleteRecursively(systemJavono, "system Javono folder");
            } else {
                LoggerFacade.getInstance().info("Skipped deleting system Javono folder.");
            }
        }

        // Update PATH (only removes bin entry for this session or future sessions via setx on Windows)
        File binDir = new File(userJavono, "bin");
        try {
            if (os.isWindows()) {
                // Remove from user PATH in Windows via setx (best effort, cannot remove from current session)
                new ProcessBuilder("cmd", "/c", "setx PATH \"%PATH:;" + binDir.getAbsolutePath() + "=%\"")
                        .inheritIO().start().waitFor();
                LoggerFacade.getInstance().info("Removed Javono bin from PATH. Close/reopen terminal to take effect.");
            } else {
                LoggerFacade.getInstance().info("For macOS/Linux, remove '" + binDir.getAbsolutePath() + "' from PATH manually.");
            }
        } catch (Exception e) {
            LoggerFacade.getInstance().error("Failed to update PATH: " + e.getMessage());
        }

        LoggerFacade.getInstance().info("Javono uninstallation completed.");
    }

    private static void deleteRecursively(File file, String description) {
        if (file == null || !file.exists()) return;

        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteRecursively(f, description);
                }
            }
        }
        if (file.delete()) {
            LoggerFacade.getInstance().info("Deleted " + description + ": " + file.getAbsolutePath());
        } else {
            LoggerFacade.getInstance().error("Failed to delete " + description + ": " + file.getAbsolutePath());
        }
    }

    public static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder()) // delete children first
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }


}