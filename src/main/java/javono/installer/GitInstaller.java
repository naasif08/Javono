package javono.installer;

import javono.detector.OS;
import javono.detector.PathDetector;
import javono.logger.Logger;
import javono.utils.TerminalLauncher;
import javono.utils.FileDownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Locale;

public class GitInstaller {


    // Lazily initialized ESP-IDF path
    private static Path espIdfPath;

    public static void ensureGitInstalled() {
        // Lazy init ESP-IDF path with validation
        if (espIdfPath == null) {
            espIdfPath = getEspIdfPath();
        }

        if (isGitOnPath()) {
            Logger.success("System Git found on PATH.");
            return;
        }

        OS os = OS.detect();
        Logger.warn("Git not found in system PATH.");

        try {
            switch (os) {
                case WINDOWS -> {
                    Logger.info("Installing Portable Git for Windows...");
                    installPortableGitWindows();
                }
                case LINUX -> {
                    Logger.info("Please install Git using your package manager.");
                    TerminalLauncher.openSudoCommandInTerminal("sudo apt-get update && sudo apt-get install -y git");
                }
                case MACOS -> {
                    Logger.info("Please install Git using Homebrew.");
                    TerminalLauncher.openSudoCommandInTerminal("brew install git");
                }
                default -> throw new UnsupportedOperationException("Unsupported OS: " + os + ". Please install Git manually.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Git installation failed", e);
        }

        // Re-check git presence after install attempt
        if (!isGitOnPath()) {
            throw new RuntimeException("Git installation failed or was not detected after installation attempt.");
        }

        Logger.success("Git installation verified.");
    }

    private static boolean isGitOnPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                return output != null && output.toLowerCase(Locale.ENGLISH).contains("git version");
            } finally {
                process.waitFor();
            }
        } catch (Exception e) {
            Logger.warn("Exception checking git presence: " + e.getMessage());
            return false;
        }
    }

    private static void installPortableGitWindows() throws IOException, InterruptedException {
        Path gitExePath = getGitExecutablePath();
        Path downloadFile = espIdfPath.resolve(getDownloadFilename());

        if (Files.exists(gitExePath)) {
            Logger.success("Portable Git already installed at: " + gitExePath);
            return;
        }

        if (Files.exists(downloadFile)) {
            Logger.warn("Previous archive found. Deleting...");
            Files.delete(downloadFile);
        }

        String downloadUrl = getDownloadUrl();
        Logger.info("Downloading Portable Git from: " + downloadUrl);
        FileDownloader.downloadWithResume(downloadUrl, downloadFile);

        Path oldGitFolder = espIdfPath.resolve("cmd");
        if (Files.exists(oldGitFolder)) {
            Logger.info("Cleaning old Git folder: " + oldGitFolder);
            deleteRecursively(oldGitFolder);
        }

        Logger.info("Extracting Portable Git archive...");
        extract7zExe(downloadFile, espIdfPath);

        Files.deleteIfExists(downloadFile);

        if (!Files.exists(gitExePath)) {
            throw new RuntimeException("Portable Git extraction failed, git.exe not found at expected location.");
        }

        Logger.success("Portable Git installed at: " + gitExePath);
    }

    private static Path getEspIdfPath() {
        String idfPathStr = PathDetector.detectIdfPath();
        if (idfPathStr == null) {
            throw new RuntimeException("ESP-IDF path not found! Please ensure ESP-IDF is installed and configured.");
        }
        return Path.of(idfPathStr);
    }

    private static Path getGitExecutablePath() {
        OS os = OS.detect();
        if (os == OS.WINDOWS) {
            return espIdfPath.resolve("cmd").resolve("git.exe");
        } else {
            // For Linux/Mac, system git is expected, no local git executable path
            return null;
        }
    }

    private static String getDownloadFilename() {
        OS os = OS.detect();
        return os == OS.WINDOWS ? "PortableGit-2.42.0-64-bit.7z.exe" : null;
    }

    private static String getDownloadUrl() {
        OS os = OS.detect();
        return os == OS.WINDOWS
                ? "https://github.com/git-for-windows/git/releases/download/v2.42.0.windows.1/PortableGit-2.42.0-64-bit.7z.exe"
                : null;
    }

    private static void extract7zExe(Path exePath, Path outputDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                exePath.toAbsolutePath().toString(),
                "-y",
                "-o" + outputDir.toAbsolutePath().toString()
        );
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("7z.exe extraction failed with exit code " + exitCode);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }
}
