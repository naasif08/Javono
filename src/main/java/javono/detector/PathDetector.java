package javono.detector;

import com.fazecast.jSerialComm.SerialPort;
import javono.logger.LoggerFacade;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;


class PathDetector {

    private final Path IDF_ROOT = getDefaultIdfPath();
    public final String VERSION = "v5.4.2";
    private final String CONSTRAINT = "v5.4";

    private Path getDefaultIdfPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Paths.get("C:", "Javono", "esp-idf-" + VERSION);
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono", "esp-idf");
        }
    }

    private Path getDefaultPath() {
        if (OS.detect().isWindows()) {
            return Paths.get("C:", "Javono", "esp-idf-" + VERSION);
        } else {
            return Paths.get(System.getProperty("user.home"), "Javono");
        }
    }

    public String detectIdfPath() {
        return Files.exists(IDF_ROOT) ? IDF_ROOT.toString() : null;
    }

    public String detectTool(String executableName) {
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

    public String detectPythonPath() {
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

    public String detectPythonExecutable() {
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

    public String detectToolchainBin() {
        return detectTool(isWindows() ? "xtensa-esp-elf-gcc.exe" : "xtensa-esp-elf-gcc");
    }

    public String detectCcacheBin() {
        return detectTool(isWindows() ? "ccache.exe" : "ccache");
    }

    public String findEspressifGitPath() {
        return DetectorFacade.getInstance().findGitPath(Path.of(Objects.requireNonNull(detectIdfPath())));
    }

    public String detectCmakePath() {
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

    public String detectNinjaPath() {
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

    public String detectIdfPyPath() {
        File idfPy = new File(IDF_ROOT.toFile(), "tools/idf.py");
        return idfPy.exists() ? idfPy.getParent() : null;
    }

    public String detectXtensaGdbPath() {
        return detectTool(isWindows() ? "xtensa-esp32-elf-gdb.exe" : "xtensa-esp32-elf-gdb");
    }

    public String detectXtensaToolchainPath() {
        return detectTool(isWindows() ? "xtensa-esp-elf-gcc.exe" : "xtensa-esp-elf-gcc");
    }

    public String detectDfuUtilBin() {
        return detectTool(isWindows() ? "dfu-util.exe" : "dfu-util");
    }

    public String detectOpenOcdBin() {
        return detectTool(isWindows() ? "openocd.exe" : "openocd");
    }

    public String detectOpenOcdScriptsPath() {
        File file = searchFileRecursively(IDF_ROOT.toFile(), "memory.tcl");
        return (file != null) ? file.getParent() : null;
    }

    public String detectEspClangPath() {
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

    private String getSystemPath(String name) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("which", name);
        Process process = pb.start();

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            process.waitFor();
            return line;
        }
    }

    public String getConstraintFilePath() {
        String FILE_NAME = "espidf.constraints." + CONSTRAINT + ".txt";
        return detectTool(FILE_NAME);
    }

    public void printDetectedPaths() {
        LoggerFacade.getInstance().info("----- Javono Detection Report -----");
        LoggerFacade.getInstance().info("IDF Path → " + detectIdfPath());
        LoggerFacade.getInstance().info("idf.py Path → " + detectIdfPyPath());
        LoggerFacade.getInstance().info("Python Path → " + detectPythonPath());
        LoggerFacade.getInstance().info("Python Executable → " + detectPythonExecutable());
        LoggerFacade.getInstance().info("Toolchain Bin → " + detectToolchainBin());
        LoggerFacade.getInstance().info("Ccache Bin → " + detectCcacheBin());
        LoggerFacade.getInstance().info("Git Path → " + findEspressifGitPath());
        LoggerFacade.getInstance().info("CMake Path → " + detectCmakePath());
        LoggerFacade.getInstance().info("Ninja Path → " + detectNinjaPath());
        LoggerFacade.getInstance().info("Xtensa GDB → " + detectXtensaGdbPath());
        LoggerFacade.getInstance().info("Xtensa Toolchain Path → " + detectXtensaToolchainPath());
        LoggerFacade.getInstance().info("DFU Util Bin → " + detectDfuUtilBin());
        LoggerFacade.getInstance().info("OpenOCD Bin → " + detectOpenOcdBin());
        LoggerFacade.getInstance().info("OpenOCD Scripts → " + detectOpenOcdScriptsPath());
        LoggerFacade.getInstance().info("Constraint File → " + getConstraintFilePath());
        LoggerFacade.getInstance().info("ESP32 Serial Port → " + detectEsp32Port());
        LoggerFacade.getInstance().info("---------------Ends---------------");
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private File searchFileRecursively(File dir, String targetFileName) {
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
