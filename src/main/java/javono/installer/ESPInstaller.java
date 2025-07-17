package javono.installer;

import javono.logger.JavonoLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ESPInstaller {

    private static final Path ESP_IDF_PATH = getDefaultInstallPath();
    private static final Path PYTHON_PATH = ESP_IDF_PATH.resolve("python-embed").resolve(getPythonBinary());
    private static final Path GIT_PATH = ESP_IDF_PATH.resolve("portable-git").resolve("cmd");

    private static final Path TOOLS_PATH = ESP_IDF_PATH.resolve(".espressif").resolve("tools");
    private static final Path PYTHON_ENV_PATH = ESP_IDF_PATH.resolve(".espressif").resolve("python_env");

    private static Path getDefaultInstallPath() {
        String os = detectOS();
        if ("windows".equals(os)) {
            return Paths.get("C:", "Javono", "esp-idf-v5.4.2");
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono", "esp-idf-v5.4.2");
        }
    }

    public static void runInstallScript() throws IOException, InterruptedException {
        String os = detectOS();

        Path scriptPath;
        List<String> command;

        if ("windows".equals(os)) {
            scriptPath = ESP_IDF_PATH.resolve("install.bat");
            command = Arrays.asList("cmd.exe", "/c", scriptPath.toString());
        } else if ("linux".equals(os) || "macos".equals(os)) {
            scriptPath = ESP_IDF_PATH.resolve("install.sh");
            command = Arrays.asList("/bin/bash", scriptPath.toString());
        } else {
            throw new IOException("Unsupported OS: " + os);
        }

        if (!Files.exists(scriptPath)) {
            throw new IOException("Install script not found: " + scriptPath);
        }

        JavonoLogger.info("Running ESP-IDF install script: " + scriptPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(ESP_IDF_PATH.toFile());
        pb.inheritIO(); // show output live in console

        // --- Environment setup for full self-contained install ---
        pb.environment().put("IDF_PATH", ESP_IDF_PATH.toString());
        pb.environment().put("IDF_TOOLS_PATH", TOOLS_PATH.toString());
        pb.environment().put("IDF_PYTHON_ENV_PATH", PYTHON_ENV_PATH.toString());

        // Optional: make Python and Git portable on Windows
        if ("windows".equals(os)) {
            pb.environment().put("PYTHON", PYTHON_PATH.toString());
            String originalPath = System.getenv("PATH");
            String combinedPath = PYTHON_PATH.getParent() + ";" + GIT_PATH + ";" + originalPath;
            pb.environment().put("PATH", combinedPath);
        }

        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Install script failed with exit code: " + exitCode);
        }

        JavonoLogger.success("ESP-IDF install script completed successfully.");
    }

    private static String detectOS() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osName.contains("win")) return "windows";
        if (osName.contains("mac")) return "macos";
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) return "linux";
        return "unknown";
    }

    private static String getPythonBinary() {
        String os = detectOS();
        return os.equals("windows") ? "python.exe" : "bin/python3";
    }
}
