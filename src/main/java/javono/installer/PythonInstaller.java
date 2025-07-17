package javono.installer;

import javono.logger.JavonoLogger;
import javono.utils.FileDownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PythonInstaller {
    private static final Path ESP_IDF_PATH = Paths.get("C:", "Javono", "esp-idf-v5.4.2");
    private static final Path INSTALL_DIR = ESP_IDF_PATH.resolve("python-embed");

    public static void ensureMinicondaInstalled() throws IOException, InterruptedException {
        Path pythonExe = getPythonExecutable();
        if (Files.exists(pythonExe)) {
            JavonoLogger.info("Miniconda Python already installed at: " + pythonExe);
            return;
        }

        String os = detectOS();
        String arch = detectArch();
        String url = getMinicondaDownloadUrl(os, arch);
        if (url == null) throw new IOException("Unsupported OS/arch: " + os + "/" + arch);

        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path installerPath = Paths.get(filename);

        JavonoLogger.info("Downloading Miniconda from: " + url);
        FileDownloader.downloadWithResume(url, installerPath);

        if ("windows".equals(os)) {
            runWindowsInstaller(installerPath);
        } else {
            runUnixInstaller(installerPath);
        }

        Files.deleteIfExists(installerPath);
        JavonoLogger.success("Miniconda installed successfully at: " + pythonExe);
    }

    private static void runWindowsInstaller(Path installerPath) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(
                installerPath.toString(),
                "/S",                                // Silent mode
                "/InstallationType=JustMe",
                "/AddToPath=0",
                "/RegisterPython=0",
                "/D=" + INSTALL_DIR.toString().replace("/", "\\") // Windows needs backslashes
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) throw new IOException("Miniconda installer failed with exit code: " + exit);
    }

    private static void runUnixInstaller(Path installerPath) throws IOException, InterruptedException {
        Files.setPosixFilePermissions(installerPath, PosixFilePermissions.fromString("rwxr-xr-x"));

        List<String> command = Arrays.asList(
                installerPath.toString(),
                "-b",                                // Batch (no prompt)
                "-p", INSTALL_DIR.toString()
        );


        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "Miniconda3-latest-Linux-x86_64.sh",
                "-b",
                "-p",
                installerPath.toAbsolutePath().toString()
        );


        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) throw new IOException("Miniconda installer failed with exit code: " + exit);
    }

    private static String getMinicondaDownloadUrl(String os, String arch) {
        if (!"x86_64".equals(arch)) return null;

        switch (os) {
            case "windows":
                return "https://repo.anaconda.com/miniconda/Miniconda3-latest-Windows-x86_64.exe";
            case "linux":
                return "https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh";
            case "macos":
                return "https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-x86_64.sh";
            default:
                return null;
        }
    }

    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "macos";
        if (os.contains("nux") || os.contains("nix")) return "linux";
        return "unknown";
    }

    private static String detectArch() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        return arch.contains("64") ? "x86_64" : "unknown";
    }

    private static Path getPythonExecutable() {
        String os = detectOS();
        if ("windows".equals(os)) {
            return INSTALL_DIR.resolve("python.exe");
        } else {
            return INSTALL_DIR.resolve("bin").resolve("python3");
        }
    }
}
