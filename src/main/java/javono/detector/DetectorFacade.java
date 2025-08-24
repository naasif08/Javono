package javono.detector;

import javono.logger.LoggerFacade;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DetectorFacade {
    public static final String VERSION = "v5.4.2";
    private static final DetectorFacade INSTANCE = new DetectorFacade();
    private static final GitPathFinder gitPathFinder = new GitPathFinder();
    private static final PathDetector pathDetector = new PathDetector();
    private static final ToolPaths toolPaths = new ToolPaths();

    private DetectorFacade() {
    }

    public static DetectorFacade getInstance() {
        return INSTANCE;
    }

    public String findGitPath(Path espIdfRoot) {
        return gitPathFinder.findGitPath(espIdfRoot);
    }

    public String detectIdfPath() {
        return pathDetector.detectIdfPath();
    }

    public String detectEsp32Port() {
        return PathDetector.detectEsp32Port();
    }

    public void printDetectedPaths() {
        pathDetector.printDetectedPaths();
    }

    public void initializeToolPaths() {
        toolPaths.init();
    }

    public boolean isToolPathsInitialized() {
        return toolPaths.isInitialized();
    }

    public File getDotJavonoDir() {
        return toolPaths.getDotJavonoDir();
    }

    public String getIdfPath() {
        return toolPaths.getIdfPath();
    }

    public String getPythonPath() {
        return toolPaths.getPythonPath();
    }

    public String getPythonExecutablePath() {
        return toolPaths.getPythonExecutablePath();
    }

    public String getToolchainPath() {
        return toolPaths.getToolchainPath();
    }

    public String getSerialPort() {
        return toolPaths.getSerialPort();
    }

    public String getGitPath() {
        return toolPaths.getGitPath();
    }

    public String getXtensaGdbPath() {
        return toolPaths.getXtensaGdbPath();
    }

    public String getXtensaToolchainPath() {
        return toolPaths.getXtensaToolchainPath();
    }

    public String getEspClangPath() {
        return toolPaths.getEspClangPath();
    }

    public String getcMakePath() {
        return toolPaths.getcMakePath();
    }

    public String getOpenOcdBin() {
        return toolPaths.getOpenOcdBin();
    }

    public String getNinjaPath() {
        return toolPaths.getNinjaPath();
    }

    public String getIdfPyPath() {
        return toolPaths.getIdfPyPath();
    }

    public String getcCacheBinPath() {
        return toolPaths.getcCacheBinPath();
    }

    public String getDfuUtilBinPath() {
        return toolPaths.getDfuUtilBinPath();
    }

    public String getOpenOcdScriptsPath() {
        return toolPaths.getOpenOcdScriptsPath();
    }

    public String getConstraintsPath() {
        return toolPaths.getConstraintsPath();
    }

    public File getProjectDir(String projectName) {
        return toolPaths.getProjectDir(projectName);
    }

    public Path findEspressifGitPath() {
        // 1. Default location: ~/.javono/espressif
        String userHome = System.getProperty("user.home");
        File defaultPath = new File(userHome, ".javono/espressif");

        if (defaultPath.exists() && defaultPath.isDirectory()) {
            LoggerFacade.getInstance().info("Found toolchain at " + defaultPath.getAbsolutePath());
            return defaultPath.toPath();
        }

        // 2. Environment variable override
        String espressifEnv = System.getenv("ESPRESSIF_GIT");
        if (espressifEnv != null && !espressifEnv.isBlank()) {
            Path envPath = Paths.get(espressifEnv);
            if (envPath.toFile().exists()) {
                LoggerFacade.getInstance().info("Found toolchain from ESPRESSIF_GIT=" + envPath);
                return envPath;
            }
        }

        // 3. Nothing found → fail with clear message
        LoggerFacade.getInstance().error(
                "Toolchain not found.\n" +
                        "Please install Javono by running: javono init\n" +
                        "This will set up required files in ~/.javono (Linux/macOS) or %USERPROFILE%\\.javono (Windows)."
        );
        throw new IllegalStateException("Javono toolchain not installed.");
    }
}
