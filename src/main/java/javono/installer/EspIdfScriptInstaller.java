package javono.installer;

import javono.detector.DetectorFacade;
import javono.detector.OS;
import javono.logger.LoggerFacade;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javono.detector.DetectorFacade.VERSION;

class EspIdfScriptInstaller {

    private static final Path ESP_IDF_PATH = getDefaultInstallPath();
    private static final Path PYTHON_PATH = ESP_IDF_PATH.resolve("python-embed").resolve(getPythonBinary());
    private static final Path GIT_PATH = ESP_IDF_PATH.resolve("portable-git").resolve("cmd");
    private static final Path PATH_UNIX = Path.of(Stream.of(DetectorFacade.getInstance().getConstraintsPath(), DetectorFacade.getInstance().getXtensaGdbPath(), DetectorFacade.getInstance().getXtensaToolchainPath(), DetectorFacade.getInstance().getcMakePath(), DetectorFacade.getInstance().getOpenOcdBin(), DetectorFacade.getInstance().getNinjaPath(), DetectorFacade.getInstance().getIdfPyPath(), DetectorFacade.getInstance().getcCacheBinPath(), DetectorFacade.getInstance().getDfuUtilBinPath(), DetectorFacade.getInstance().getPythonPath(), DetectorFacade.getInstance().getOpenOcdScriptsPath()).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(":")));
    private static final Path PATH = Path.of(Stream.of(DetectorFacade.getInstance().getConstraintsPath(), DetectorFacade.getInstance().getXtensaGdbPath(), DetectorFacade.getInstance().getXtensaToolchainPath(), DetectorFacade.getInstance().getcMakePath(), DetectorFacade.getInstance().getOpenOcdBin(), DetectorFacade.getInstance().getNinjaPath(), DetectorFacade.getInstance().getIdfPyPath(), DetectorFacade.getInstance().getcCacheBinPath(), DetectorFacade.getInstance().getDfuUtilBinPath(), DetectorFacade.getInstance().getPythonPath(), DetectorFacade.getInstance().getOpenOcdScriptsPath()).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(";")));
    private static final Path TOOLS_PATH = ESP_IDF_PATH.resolve(".espressif").resolve("tools");
    private static final Path PYTHON_ENV_PATH = ESP_IDF_PATH.resolve(".espressif").resolve("python_env");

    public static Path getDefaultInstallPath() {
        String os = detectOS();
        if ("windows".equals(os)) {
            return Paths.get("C:", "Javono", "esp-idf-" + VERSION);
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono", "esp-idf-" + VERSION);
        }
    }

    public void runInstallScript() throws IOException, InterruptedException {
        OS os = OS.detect();

        Path scriptPath;
        List<String> command;

        if (os == OS.WINDOWS) {
            scriptPath = ESP_IDF_PATH.resolve("install.bat");
            command = Arrays.asList("cmd.exe", "/c", scriptPath.toString());
        } else if (os == OS.LINUX || os == OS.MACOS) {
            scriptPath = ESP_IDF_PATH.resolve("install.sh");
            command = Arrays.asList("/bin/bash", scriptPath.toString());
        } else {
            throw new IOException("Unsupported OS: " + os);
        }

        if (!Files.exists(scriptPath)) {
            throw new IOException("Install script not found: " + scriptPath);
        }

        LoggerFacade.getInstance().info("Running ESP-IDF install script: " + scriptPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(ESP_IDF_PATH.toFile());
        pb.inheritIO(); // show output live in console

        // --- Environment setup for full self-contained install ---
        pb.environment().put("IDF_PATH", ESP_IDF_PATH.toString());
        pb.environment().put("IDF_TOOLS_PATH", TOOLS_PATH.toString());
        pb.environment().put("IDF_PYTHON_ENV_PATH", PYTHON_ENV_PATH.toString());
        String systemPath = System.getenv("PATH");
        String combinedPath;
        if (os == OS.WINDOWS) {
            combinedPath = PATH + ";" + systemPath;  // ";" for Windows
        } else {
            combinedPath = PATH_UNIX + ":" + systemPath;  // ":" for Linux/macOS
        }
        pb.environment().put("PATH", combinedPath);

        // Optional: make Python and Git portable on Windows
        if (os == OS.WINDOWS) {
            pb.environment().put("PYTHON", PYTHON_PATH.toString());
            Path miniconda = ESP_IDF_PATH.resolve("miniconda");
            Path scripts = miniconda.resolve("Scripts");
            Path libBin = miniconda.resolve("Library").resolve("bin");

            String newPath = String.join(";",
                    miniconda.toString(),
                    scripts.toString(),
                    libBin.toString(),
                    GIT_PATH.toString(),
                    System.getenv("PATH")
            );

            pb.environment().put("PATH", newPath);
            pb.environment().put("PYTHON", miniconda.resolve("python.exe").toString());

        }

        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            LoggerFacade.getInstance().error("Install script exited with code " + exitCode);
            throw new RuntimeException("ESP-IDF installation failed or incomplete.");
        }

        LoggerFacade.getInstance().success("ESP-IDF install script completed successfully.");
    }

    private static String detectOS() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osName.contains("win")) return "windows";
        if (osName.contains("mac")) return "macos";
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) return "linux";
        return "unknown";
    }

    public static String getPythonBinary() {
        String os = detectOS();
        return os.equals("windows") ? "python.exe" : "bin/python3";
    }
}
