package javono.probuilder;

import javono.detector.ToolPaths;
import javono.logger.JavonoLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BatchBuilder {

    private static final String IDF_PATH = ToolPaths.idfPath;
    private static final String PYTHON_EXE_PATH = ToolPaths.pythonExecutablePath;
    private static final String CONSTRAINTS_PATH = ToolPaths.constraintsPath;
    private static final String PATH = Stream.of(ToolPaths.xtensaGdbPath, ToolPaths.xtensaToolchainPath, ToolPaths.cMakePath, ToolPaths.openOcdBin, ToolPaths.ninjaPath, ToolPaths.idfPyPath, ToolPaths.cCacheBinPath, ToolPaths.dfuUtilBinPath, ToolPaths.pythonPath, ToolPaths.openOcdScriptsPath).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(";"));

    private static final String OPENOCD_SCRIPTS = ToolPaths.openOcdScriptsPath;
    private static final String GIT_PATH = ToolPaths.gitPath;

    public void writeBuildScripts(File projectDir, String comPort) throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            writeBatchFile(projectDir, comPort);
        } else if (osName.contains("mac") || osName.contains("nix") || osName.contains("nux")) {
            writeBashScript(projectDir, comPort);
        } else {
            throw new UnsupportedOperationException("Unsupported OS for script generation: " + osName);
        }
    }

    private void writeBatchFile(File projectDir, String comPort) throws IOException {
        File batchFile = new File(projectDir, "esp32_build_flash.bat");
        File pythonExeFile = new File(PYTHON_EXE_PATH);
        File pythonEnvDir = pythonExeFile.getParentFile().getParentFile();
        String idfPythonEnvPath = pythonEnvDir.getAbsolutePath();
        String batchContent = """
                @echo off
                REM === Javono ESP32 Build & Flash Script ===
                
                REM Set ESP-IDF environment
                set "IDF_PATH=%s"
                set "OPENOCD_SCRIPTS=%s"
                set "PYTHON_EXE_PATH=%s"
                set "IDF_PYTHON_ENV_PATH=%s"
                set "IDF_TOOLS_PATH=%s"
                set "GIT_PATH=%s"
                
                REM Add required tools to PATH
                set "PATH=%s"
                
                REM Move to project directory
                cd /d "%s"
                
                echo 🚀 Building project...
                call "%%PYTHON_EXE_PATH%%" "%%IDF_PATH%%\\tools\\idf.py" build
                if errorlevel 1 (
                    echo ❌ Build failed! Exiting...
                    pause
                    exit /b 1
                )
                
                echo 📦 Flashing firmware to %s...
                call "%%PYTHON_EXE_PATH%%" "%%IDF_PATH%%\\tools\\idf.py" -p %s flash
                if errorlevel 1 (
                    echo ❌ Flash failed! Exiting...
                    pause
                    exit /b 1
                )
                
                echo ✅ Operation completed successfully.
                """.formatted(
                IDF_PATH,
                OPENOCD_SCRIPTS,
                PYTHON_EXE_PATH,
                idfPythonEnvPath,  // derived from PYTHON_EXE_PATH folder, as before
                CONSTRAINTS_PATH,
                GIT_PATH,
                PATH,
                projectDir.getAbsolutePath(),
                comPort,
                comPort
        );


        try (FileWriter writer = new FileWriter(batchFile)) {
            writer.write(batchContent);
        }

        JavonoLogger.success("Batch file written to: " + batchFile.getAbsolutePath());

    }

    private void writeBashScript(File projectDir, String comPort) throws IOException {
        File bashFile = new File(projectDir, "esp32_build_flash.sh");
        String bashContent = """               
                #!/bin/bash
                
                # === Configuration ===
                IDF_PATH="%s"
                PROJECT_DIR="%s"
                
                # Load ESP-IDF environment
                source "$IDF_PATH/export.sh"
                
                # === Build the project ===
                echo "🔨 Building the project..."
                idf.py build
                if [ $? -ne 0 ]; then
                    echo "❌ Build failed!"
                    exit 1
                fi
                
                # === Flash the project ===
                COM_PORT="%s"
                if [ -z "$COM_PORT" ]; then
                    echo "❌ COM port not specified. Usage: $0 <com_port>"
                    exit 1
                fi
                
                echo "🚀 Flashing to $COM_PORT..."                
                idf.py -p "$COM_PORT" flash              
                if [ $? -ne 0 ]; then
                    echo "❌ Flash failed!"
                    exit 1
                fi
                
                echo "✅ Build and flash completed."
                
                """.formatted(
                ToolPaths.idfPath,
                projectDir.getAbsolutePath(),
                comPort,
                comPort
        );

        try (FileWriter writer = new FileWriter(bashFile)) {
            writer.write(bashContent);
        }

        bashFile.setExecutable(true); // Make script executable

        JavonoLogger.success("Bash script written to: " + bashFile.getAbsolutePath());
    }


}
