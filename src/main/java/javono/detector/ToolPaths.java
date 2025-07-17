package javono.detector;

import javono.logger.JavonoLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ToolPaths {

    private static boolean initialized = false;

    // Public static paths
    public static String idfPath;
    public static String pythonPath;
    public static String pythonExecutablePath;
    public static String toolchainPath;
    public static String serialPort;
    public static String gitPath;
    public static String xtensaGdbPath;
    public static String xtensaToolchainPath;
    public static String espClangPath;
    public static String cMakePath;
    public static String openOcdBin;
    public static String ninjaPath;
    public static String idfPyPath;
    public static String cCacheBinPath;
    public static String dfuUtilBinPath;
    public static String openOcdScriptsPath;
    public static String constraintsPath;

    private static File dotJavonoDir;

    public static void init() {
        if (initialized) return;

        dotJavonoDir = new File(System.getProperty("user.dir"), ".javono");
        if (!dotJavonoDir.exists()) dotJavonoDir.mkdirs();

        ensureJavonoPropertiesTemplate();

        idfPath = PathDetector.detectIdfPath();
        pythonPath = PathDetector.detectPythonPath();
        pythonExecutablePath = PathDetector.detectPythonExecutable();
        toolchainPath = PathDetector.detectToolchainBin();
        serialPort = PathDetector.detectEsp32Port();
        gitPath = PathDetector.detectEspressifGitPath();
        xtensaGdbPath = PathDetector.detectXtensaGdbPath();
        xtensaToolchainPath = PathDetector.detectXtensaToolchainPath();
        cMakePath = PathDetector.detectCmakePath();
        openOcdBin = PathDetector.detectOpenOcdBin();
        ninjaPath = PathDetector.detectNinjaPath();
        idfPyPath = PathDetector.detectIdfPyPath();
        cCacheBinPath = PathDetector.detectCcacheBin();
        dfuUtilBinPath = PathDetector.detectDfuUtilBin();
        openOcdScriptsPath = PathDetector.detectOpenOcdScriptsPath();
        constraintsPath = PathDetector.getConstraintFilePath();

        loadPropertiesOverrides();
        validatePaths();

        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static File getDotJavonoDir() {
        if (dotJavonoDir == null) {
            dotJavonoDir = new File(System.getProperty("user.dir"), ".Javono");
            if (!dotJavonoDir.exists()) {
                boolean created = dotJavonoDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("❌ Failed to create/access .Javono directory: " + dotJavonoDir.getAbsolutePath());
                }
            }
        }
        return dotJavonoDir;
    }

    public static File getProjectDir(String projectName) {
        Path projectDir = getDotJavonoDir().toPath().resolve(projectName);
        try {
            Files.createDirectories(projectDir);
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to create/access project directory: " + projectDir, e);
        }
        return projectDir.toFile();
    }

    private static void ensureJavonoPropertiesTemplate() {
        Path propPath = getDotJavonoDir().toPath().resolve("javono.properties");
        if (Files.exists(propPath)) return;

        String template = """
                # Javono Properties Template
                # Fill manually if auto-detection fails
                
                javono.idfPath=
                javono.idfPyPath=
                javono.pythonPath=
                javono.pythonExecutablePath=
                javono.toolchainPath=
                javono.cMakePath=
                javono.ninjaPath=
                javono.constraintsPath=
                javono.serialPort=
                
                javono.gitPath=
                javono.xtensaGdbPath=
                javono.xtensaToolchainPath=
                javono.espClangPath=
                javono.openOcdBin=
                javono.cCacheBinPath=
                javono.dfuUtilBinPath=
                javono.openOcdScriptsPath=
                """;

        try {
            Files.writeString(propPath, template);
            JavonoLogger.success("Created .Javono/javono.properties template.");
        } catch (IOException e) {
            JavonoLogger.error("Failed to create javono.properties: " + e.getMessage());
        }
    }

    private static void loadPropertiesOverrides() {
        Path propPath = getDotJavonoDir().toPath().resolve("javono.properties");
        if (!Files.exists(propPath)) return;

        try (InputStream in = Files.newInputStream(propPath)) {
            Properties props = new Properties();
            props.load(in);
            JavonoLogger.info("Loaded manual overrides from javono.properties");

            idfPath = resolve(props, "javono.idfPath", idfPath);
            pythonPath = resolve(props, "javono.pythonPath", pythonPath);
            pythonExecutablePath = resolve(props, "javono.pythonExecutablePath", pythonExecutablePath);
            toolchainPath = resolve(props, "javono.toolchainPath", toolchainPath);
            serialPort = resolve(props, "javono.serialPort", serialPort);
            gitPath = resolve(props, "javono.gitPath", gitPath);
            xtensaGdbPath = resolve(props, "javono.xtensaGdbPath", xtensaGdbPath);
            xtensaToolchainPath = resolve(props, "javono.xtensaToolchainPath", xtensaToolchainPath);
            espClangPath = resolve(props, "javono.espClangPath", espClangPath);
            cMakePath = resolve(props, "javono.cMakePath", cMakePath);
            openOcdBin = resolve(props, "javono.openOcdBin", openOcdBin);
            ninjaPath = resolve(props, "javono.ninjaPath", ninjaPath);
            idfPyPath = resolve(props, "javono.idfPyPath", idfPyPath);
            cCacheBinPath = resolve(props, "javono.cCacheBinPath", cCacheBinPath);
            dfuUtilBinPath = resolve(props, "javono.dfuUtilBinPath", dfuUtilBinPath);
            openOcdScriptsPath = resolve(props, "javono.openOcdScriptsPath", openOcdScriptsPath);
            constraintsPath = resolve(props, "javono.constraintsPath", constraintsPath);

        } catch (IOException e) {
            System.err.println("⚠️ Failed to read javono.properties: " + e.getMessage());
        }
    }

    private static String resolve(Properties props, String key, String fallback) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null")) {
            return fallback;
        }
        return value.trim();
    }


    private static void validatePaths() {
        check("idfPath", idfPath);
        check("idfPyPath", idfPyPath);
        check("pythonPath", pythonPath);
        check("pythonExecutablePath", pythonExecutablePath);
        check("toolchainPath", toolchainPath);
        check("cMakePath", cMakePath);
        check("ninjaPath", ninjaPath);
        check("constraintsPath", constraintsPath);
        check("serialPort", serialPort);
    }

    private static void check(String name, String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            System.err.println("❌ Missing required path: javono." + name);
            System.err.println("→ Fix it in .Javono/javono.properties or set it manually.");
            System.exit(1);
        }
    }

    private ToolPaths() {
    } // prevent instantiation
}
