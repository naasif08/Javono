package javono.detector;


import javono.logger.LoggerFacade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class ToolPaths {

    private static boolean initialized = false;
    private static PathDetector pathDetector = new PathDetector();

    // Public static paths
    private String idfPath;
    private String pythonPath;
    private String pythonExecutablePath;
    private String toolchainPath;
    private String serialPort;
    private String gitPath;
    private String xtensaGdbPath;
    private String xtensaToolchainPath;
    private String espClangPath;
    private String cMakePath;
    private String openOcdBin;
    private String ninjaPath;
    private String idfPyPath;
    private String cCacheBinPath;
    private String dfuUtilBinPath;
    private String openOcdScriptsPath;
    private String constraintsPath;
    private File dotJavonoDir;

    public void init() {
        if (initialized) return;

        dotJavonoDir = new File(System.getProperty("user.dir"), ".javono");
        if (!dotJavonoDir.exists()) dotJavonoDir.mkdirs();

        ensureJavonoPropertiesTemplate();

        idfPath = pathDetector.detectIdfPath();
        pythonPath = pathDetector.detectPythonPath();
        pythonExecutablePath = pathDetector.detectPythonExecutable();
        toolchainPath = pathDetector.detectToolchainBin();
        serialPort = pathDetector.detectEsp32Port();
        gitPath = pathDetector.findEspressifGitPath();
        xtensaGdbPath = pathDetector.detectXtensaGdbPath();
        xtensaToolchainPath = pathDetector.detectXtensaToolchainPath();
        cMakePath = pathDetector.detectCmakePath();
        openOcdBin = pathDetector.detectOpenOcdBin();
        ninjaPath = pathDetector.detectNinjaPath();
        idfPyPath = pathDetector.detectIdfPyPath();
        cCacheBinPath = pathDetector.detectCcacheBin();
        dfuUtilBinPath = pathDetector.detectDfuUtilBin();
        openOcdScriptsPath = pathDetector.detectOpenOcdScriptsPath();
        constraintsPath = pathDetector.getConstraintFilePath();

        loadPropertiesOverrides();
        validatePaths();

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public File getDotJavonoDir() {
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

    public File getProjectDir(String projectName) {
        Path projectDir = getDotJavonoDir().toPath().resolve(projectName);
        try {
            Files.createDirectories(projectDir);
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to create/access project directory: " + projectDir, e);
        }
        return projectDir.toFile();
    }

    private void ensureJavonoPropertiesTemplate() {
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
            LoggerFacade.getInstance().success("Created .Javono/javono.properties template.");
        } catch (IOException e) {
            LoggerFacade.getInstance().error("Failed to create javono.properties: " + e.getMessage());
        }
    }

    private void loadPropertiesOverrides() {
        Path propPath = getDotJavonoDir().toPath().resolve("javono.properties");
        if (!Files.exists(propPath)) return;

        try (InputStream in = Files.newInputStream(propPath)) {
            Properties props = new Properties();
            props.load(in);
            LoggerFacade.getInstance().info("Loaded manual overrides from javono.properties");

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


    private void validatePaths() {
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
            LoggerFacade.getInstance().error("❌ Missing required path: javono." + name);
            LoggerFacade.getInstance().error("→ Fix it in .javono/javono.properties or set it manually");
            if (name.equals("serialPort"))
                LoggerFacade.getInstance().error("→ or you might not have connected ESP32 to your PC.");
            System.exit(1);
        }
    }

    public static PathDetector getPathDetector() {
        return pathDetector;
    }

    public String getIdfPath() {
        return idfPath;
    }

    public String getPythonPath() {
        return pythonPath;
    }

    public String getPythonExecutablePath() {
        return pythonExecutablePath;
    }

    public String getToolchainPath() {
        return toolchainPath;
    }

    public String getSerialPort() {
        return serialPort;
    }

    public String getGitPath() {
        return gitPath;
    }

    public String getXtensaGdbPath() {
        return xtensaGdbPath;
    }

    public String getXtensaToolchainPath() {
        return xtensaToolchainPath;
    }

    public String getEspClangPath() {
        return espClangPath;
    }

    public String getcMakePath() {
        return cMakePath;
    }

    public String getOpenOcdBin() {
        return openOcdBin;
    }

    public String getNinjaPath() {
        return ninjaPath;
    }

    public String getIdfPyPath() {
        return idfPyPath;
    }

    public String getcCacheBinPath() {
        return cCacheBinPath;
    }

    public String getDfuUtilBinPath() {
        return dfuUtilBinPath;
    }

    public String getOpenOcdScriptsPath() {
        return openOcdScriptsPath;
    }

    public String getConstraintsPath() {
        return constraintsPath;
    }
}
