package javono.installer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import javono.detector.PathDetector;
import javono.logger.JavonoLogger;
import javono.utils.CommandRunner;

public class CmakeInstaller {

    public static void installCmakeIfMissing(String espIdfPath) {
        JavonoLogger.info("Installing CMake using idf_tools.py...");

        File idfToolsPy = new File(espIdfPath, "tools/idf_tools.py");
        if (!idfToolsPy.exists()) {
            throw new RuntimeException("❌ idf_tools.py not found at: " + idfToolsPy.getAbsolutePath());
        }

        String pythonPath = espIdfPath + "/.espressif/python_env/bin/python";
        File pythonExe = new File(pythonPath);
        if (!pythonExe.exists()) {
            throw new RuntimeException("❌ ESP-IDF Python environment not found at: " + pythonPath);
        }

        try {
            // Set IDF_TOOLS_PATH to project-local .espressif folder
            ProcessBuilder builder = new ProcessBuilder(pythonExe.getAbsolutePath(), idfToolsPy.getAbsolutePath(), "install", "cmake");
            builder.environment().put("IDF_TOOLS_PATH", new File(espIdfPath, ".espressif").getAbsolutePath());
            builder.inheritIO(); // optional: prints live output
            int exitCode = builder.start().waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("❌ Failed to install CMake with idf_tools.py");
            }

            JavonoLogger.success("CMake installed with project-local IDF_TOOLS_PATH");
            findCmake(espIdfPath);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("❌ Error during CMake install", e);
        }
    }

    public static void findCmake(String espIdfPath) {
        File cmakeBase = new File(espIdfPath, ".espressif/tools/cmake");
        if (!cmakeBase.exists() || !cmakeBase.isDirectory()) {
            throw new RuntimeException("❌ CMake folder not found in expected path: " + cmakeBase.getAbsolutePath());
        }

        Optional<File> versionDir = Arrays.stream(cmakeBase.listFiles()).filter(File::isDirectory).filter(dir -> new File(dir, "bin/cmake").exists()).findFirst();

        if (versionDir.isPresent()) {
            File cmakeBin = new File(versionDir.get(), "bin/cmake");
            JavonoLogger.success("Found CMake binary at: " + cmakeBin.getAbsolutePath());
        } else {
            throw new RuntimeException("❌ No valid CMake version directory with binary found inside: " + cmakeBase.getAbsolutePath());
        }
    }
}
