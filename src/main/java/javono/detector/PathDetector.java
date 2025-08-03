package javono.detector;

import com.fazecast.jSerialComm.SerialPort;
import javono.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;


public class PathDetector {

    private static final Path IDF_ROOT = getDefaultIdfPath();
    public static final String VERSION = "v5.4.2";
    private static final String CONSTRAINT = "v5.4";

    private static Path getDefaultIdfPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Paths.get("C:", "Javono", "esp-idf-" + VERSION);
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono", "esp-idf");
        }
    }

    private static Path getDefaultPath() {
        if (OS.detect().isWindows()) {
            return Paths.get("C:", "Javono", "esp-idf-" + VERSION);
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono");
        }
    }

    public static String detectIdfPath() {
        return Files.exists(IDF_ROOT) ? IDF_ROOT.toString() : null;
    }

    public static String detectTool(String executableName) {
        File toolPath;
        if (OS.detect().isWindows()) {
            toolPath = searchFileRecursively(IDF_ROOT.toFile(), executableName);
        } else {
            toolPath = searchFileRecursively(getDefaultPath().toFile(), executableName);
            if (toolPath == null || toolPath.equals("null")) {
                toolPath = searchFileRecursively(Paths.get(System.getProperty("user.home"), ".espressif").toFile(), executableName);
            }
        }
        return (toolPath != null) ? toolPath.getParent() : null;
    }

    public static String detectPythonPath() {
        if (OS.detect().isWindows()) {
            return detectTool("python.exe");
        } else {
            try {
                return getSystemPath("python3");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static String detectPythonExecutable() {
        File file;
        if (OS.detect().isWindows()) {
            file = searchFileRecursively(IDF_ROOT.toFile(), "python.exe");
            return (file != null) ? file.getAbsolutePath() : null;
        } else {
            try {
                return getSystemPath("python3");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String detectToolchainBin() {
        return detectTool(isWindows() ? "xtensa-esp-elf-gcc.exe" : "xtensa-esp-elf-gcc");
    }

    public static String detectCcacheBin() {
        return detectTool(isWindows() ? "ccache.exe" : "ccache");
    }

    public static String findEspressifGitPath() {
        return GitPathFinder.findGitPath(Path.of(Objects.requireNonNull(detectIdfPath())));
    }

    public static String detectCmakePath() {
        if (OS.detect().isWindows()) {
            return detectTool("cmake.exe");
        } else {
            try {
                return getSystemPath("cmake");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String detectNinjaPath() {
        if (OS.detect().isWindows()) {
            return detectTool("ninja.exe");
        } else {
            try {
                return getSystemPath("ninja");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String detectIdfPyPath() {
        File idfPy = new File(IDF_ROOT.toFile(), "tools/idf.py");
        return idfPy.exists() ? idfPy.getParent() : null;
    }

    public static String detectXtensaGdbPath() {
        return detectTool(isWindows() ? "xtensa-esp32-elf-gdb.exe" : "xtensa-esp32-elf-gdb");
    }

    public static String detectXtensaToolchainPath() {
        return detectTool(isWindows() ? "xtensa-esp-elf-gcc.exe" : "xtensa-esp-elf-gcc");
    }

    public static String detectDfuUtilBin() {
        return detectTool(isWindows() ? "dfu-util.exe" : "dfu-util");
    }

    public static String detectOpenOcdBin() {
        return detectTool(isWindows() ? "openocd.exe" : "openocd");
    }

    public static String detectOpenOcdScriptsPath() {
        File file = searchFileRecursively(IDF_ROOT.toFile(), "memory.tcl");
        return (file != null) ? file.getParent() : null;
    }

    public static String detectEspClangPath() {
        return detectTool(isWindows() ? "clang.exe" : "clang");
    }

    public static String detectEsp32Port() {
        for (SerialPort port : SerialPort.getCommPorts()) {
            String desc = port.getDescriptivePortName().toLowerCase();
            String name = port.getSystemPortName().toLowerCase();
            if (desc.contains("ch340") || desc.contains("usb serial") || desc.contains("cp210x") || desc.contains("ftdi") || name.contains("usbserial") || name.contains("ttyusb") || name.contains("cu.usbserial")) {
                if (OS.detect().isWindows()) {
                    return port.getSystemPortName();
                } else {
                    return "/dev/" + port.getSystemPortName();
                }
            }
        }
        return null;
    }

    private static String getSystemPath(String name) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("which", name);
        Process process = pb.start();

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            process.waitFor();
            return line;
        }
    }

    public static String getConstraintFilePath() {
        String FILE_NAME = "espidf.constraints." + CONSTRAINT + ".txt";
        return detectTool(FILE_NAME);
    }

    public static void printDetectedPaths() {
        Logger.info("----- Javono Detection Report -----");
        Logger.info("IDF Path → " + detectIdfPath());
        Logger.info("idf.py Path → " + detectIdfPyPath());
        Logger.info("Python Path → " + detectPythonPath());
        Logger.info("Python Executable → " + detectPythonExecutable());
        Logger.info("Toolchain Bin → " + detectToolchainBin());
        Logger.info("Ccache Bin → " + detectCcacheBin());
        Logger.info("Git Path → " + findEspressifGitPath());
        Logger.info("CMake Path → " + detectCmakePath());
        Logger.info("Ninja Path → " + detectNinjaPath());
        Logger.info("Xtensa GDB → " + detectXtensaGdbPath());
        Logger.info("Xtensa Toolchain Path → " + detectXtensaToolchainPath());
        Logger.info("DFU Util Bin → " + detectDfuUtilBin());
        Logger.info("OpenOCD Bin → " + detectOpenOcdBin());
        Logger.info("OpenOCD Scripts → " + detectOpenOcdScriptsPath());
        Logger.info("Constraint File → " + getConstraintFilePath());
        Logger.info("ESP32 Serial Port → " + detectEsp32Port());
        Logger.info("---------------Ends---------------");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static File searchFileRecursively(File dir, String targetFileName) {
        if (dir == null || !dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                File result = searchFileRecursively(f, targetFileName);
                if (result != null) return result;
            } else if (f.getName().equalsIgnoreCase(targetFileName)) {
                return f;
            }
        }
        return null;
    }

}
