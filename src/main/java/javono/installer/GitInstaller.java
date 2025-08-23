package javono.installer;

import javono.detector.DetectorFacade;
import javono.detector.OS;
import javono.logger.LoggerFacade;
import javono.utils.UtilsFacade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Locale;

class GitInstaller {


    // Lazily initialized ESP-IDF path
    private static Path espIdfPath;

    public void ensureGitInstalled() {
        // Lazy init ESP-IDF path with validation
        if (espIdfPath == null) {
            espIdfPath = getEspIdfPath();
        }

        if (isGitOnPath()) {
            LoggerFacade.getInstance().success("System Git found on PATH.");
            return;
        }

        OS os = OS.detect();
        LoggerFacade.getInstance().warn("Git not found in system PATH.");

        try {
            switch (os) {
                case WINDOWS -> {
                    LoggerFacade.getInstance().info("Installing Portable Git for Windows...");
                    installPortableGitWindows();
                }
                case LINUX -> {
                    LoggerFacade.getInstance().info("Please install Git using your package manager.");
                    UtilsFacade.getInstance().openSudoCommandInTerminal("sudo apt-get update && sudo apt-get install -y git");
                }
                case MACOS -> {
                    LoggerFacade.getInstance().info("Please install Git using Homebrew.");
                    UtilsFacade.getInstance().openSudoCommandInTerminal("brew install git");
                }
                default ->
                        throw new UnsupportedOperationException("Unsupported OS: " + os + ". Please install Git manually.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Git installation failed", e);
        }

        // Re-check git presence after install attempt
        if (!isGitOnPath()) {
            throw new RuntimeException("Git installation failed or was not detected after installation attempt.");
        }

        LoggerFacade.getInstance().success("Git installation verified.");
    }

    private boolean isGitOnPath() {
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
            LoggerFacade.getInstance().warn("Exception checking git presence: " + e.getMessage());
            return false;
        }
    }

    private void installPortableGitWindows() throws IOException, InterruptedException {
        Path gitExePath = getGitExecutablePath();
        Path downloadFile = espIdfPath.resolve(getDownloadFilename());

        if (Files.exists(gitExePath)) {
            LoggerFacade.getInstance().success("Portable Git already installed at: " + gitExePath);
            return;
        }

        if (Files.exists(downloadFile)) {
            LoggerFacade.getInstance().warn("Previous archive found. Deleting...");
            Files.delete(downloadFile);
        }

        String downloadUrl = getDownloadUrl();
        LoggerFacade.getInstance().info("Downloading Portable Git from: " + downloadUrl);
        UtilsFacade.getInstance().downloadWithResume(downloadUrl, downloadFile);

        Path oldGitFolder = espIdfPath.resolve("cmd");
        if (Files.exists(oldGitFolder)) {
            LoggerFacade.getInstance().info("Cleaning old Git folder: " + oldGitFolder);
            deleteRecursively(oldGitFolder);
        }

        LoggerFacade.getInstance().info("Extracting Portable Git archive...");
        extract7zExe(downloadFile, espIdfPath);

        Files.deleteIfExists(downloadFile);

        if (!Files.exists(gitExePath)) {
            throw new RuntimeException("Portable Git extraction failed, git.exe not found at expected location.");
        }

        LoggerFacade.getInstance().success("Portable Git installed at: " + gitExePath);
    }

    private Path getEspIdfPath() {
        String idfPathStr = DetectorFacade.getInstance().detectIdfPath();
        if (idfPathStr == null) {
            throw new RuntimeException("ESP-IDF path not found! Please ensure ESP-IDF is installed and configured.");
        }
        return Path.of(idfPathStr);
    }

    private Path getGitExecutablePath() {
        OS os = OS.detect();
        if (os == OS.WINDOWS) {
            return espIdfPath.resolve("cmd").resolve("git.exe");
        } else {
            // For Linux/Mac, system git is expected, no local git executable path
            return null;
        }
    }

    private String getDownloadFilename() {
        OS os = OS.detect();
        return os == OS.WINDOWS ? "PortableGit-2.42.0-64-bit.7z.exe" : null;
    }

    private String getDownloadUrl() {
        OS os = OS.detect();
        return os == OS.WINDOWS ? "https://github.com/git-for-windows/git/releases/download/v2.42.0.windows.1/PortableGit-2.42.0-64-bit.7z.exe" : null;
    }

    private void extract7zExe(Path exePath, Path outputDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(exePath.toAbsolutePath().toString(), "-y", "-o" + outputDir.toAbsolutePath().toString());
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("7z.exe extraction failed with exit code " + exitCode);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
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
