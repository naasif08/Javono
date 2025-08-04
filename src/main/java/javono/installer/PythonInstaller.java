package javono.installer;


import javono.logger.LoggerFacade;
import javono.utils.UtilsFacade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static javono.detector.DetectorFacade.VERSION;

class PythonInstaller {
    private static final Path ESP_IDF_PATH = getDefaultInstallPath();
    private static final Path INSTALL_DIR = ESP_IDF_PATH.resolve("miniconda");

    private static Path getDefaultInstallPath() {
        String os = detectOS();
        if ("windows".equals(os)) {
            return Paths.get("C:", "Javono", "esp-idf-" + VERSION);
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono", "esp-idf-" + VERSION);
        }
    }

    public void ensureMinicondaInstalled() throws IOException, InterruptedException {
        Path pythonExe = getPythonExecutable();
        if (Files.exists(pythonExe)) {
            LoggerFacade.getInstance().info("Miniconda Python already installed at: " + pythonExe);
            return;
        }

        String os = detectOS();
        String arch = detectArch();
        String url = getMinicondaDownloadUrl(os, arch);
        if (url == null) throw new IOException("Unsupported OS/arch: " + os + "/" + arch);

        String filename = url.substring(url.lastIndexOf("/") + 1);
        Path installerPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(filename);

        LoggerFacade.getInstance().info("Downloading Miniconda from: " + url);
        UtilsFacade.getInstance().downloadWithResume(url, installerPath);

        if ("windows".equals(os)) {
            runWindowsInstaller(installerPath);
        } else {
            runUnixInstaller(installerPath);
        }

        Files.deleteIfExists(installerPath);
        LoggerFacade.getInstance().success("Miniconda installed successfully at: " + pythonExe);
    }

    private static void runWindowsInstaller(Path installerPath) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(installerPath.toString(), "/S",                                // Silent mode
                "/InstallationType=JustMe", "/AddToPath=0", "/RegisterPython=0", "/D=" + INSTALL_DIR.toString().replace("/", "\\") // Windows needs backslashes
        );

        LoggerFacade.getInstance().info("Running Windows Miniconda installer: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) throw new IOException("Miniconda installer failed with exit code: " + exit);
    }

    private static void ensurePythonVenvAvailable() throws IOException, InterruptedException {
        if (!"linux".equals(detectOS())) return; // Only for Linux

        ProcessBuilder checkVenv = new ProcessBuilder("python3", "-m", "venv", "--help");
        Process checkProcess = checkVenv.start();
        int checkExit = checkProcess.waitFor();

        if (checkExit == 0) {
            LoggerFacade.getInstance().info("python3-venv is already available.");
            return;
        }

        LoggerFacade.getInstance().info("python3-venv not found. Attempting to install...");

        // Try installing via apt
        ProcessBuilder install = new ProcessBuilder("sudo", "apt-get", "update");
        install.inheritIO().start().waitFor();

        ProcessBuilder installVenv = new ProcessBuilder("sudo", "apt-get", "install", "-y", "python3-venv");
        installVenv.inheritIO();
        Process installProcess = installVenv.start();
        int installExit = installProcess.waitFor();

        if (installExit != 0) {
            throw new IOException("Failed to install python3-venv via apt-get.");
        }

        LoggerFacade.getInstance().success("python3-venv installed successfully.");
    }


    private static void runUnixInstaller(Path installerPath) throws IOException, InterruptedException {
        Files.setPosixFilePermissions(installerPath, PosixFilePermissions.fromString("rwxr-xr-x"));

        List<String> command = Arrays.asList("bash", installerPath.toAbsolutePath().toString(), "-b",  // batch (silent)
                "-p", INSTALL_DIR.toString());

        LoggerFacade.getInstance().info("Running Unix Miniconda installer: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        ensurePythonVenvAvailable();
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
        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) return "linux";
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
