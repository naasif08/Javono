package javono.installer;

import javono.logger.JavonoLogger;

import java.io.IOException;
import java.util.List;

public class EspIdfInstallerUnix {

    public static void installForLinux() {
        String espDir = "$HOME/Javono"; // or use absolute path if preferred
        String fullCommand = String.join(" && ", List.of(
                "sudo apt update",
                "sudo apt install -y git wget flex bison gperf python3 python3-pip python3-setuptools cmake ninja-build ccache libffi-dev libssl-dev dfu-util",
                "mkdir -p " + espDir,
                "cd " + espDir,
                "git clone -b v5.4.2 --recursive https://github.com/espressif/esp-idf.git",
                "cd esp-idf",
                "./install.sh esp32",
                "source export.sh",
                "exec bash"
        ));

        List<String> command = List.of(
                "x-terminal-emulator", "-e",
                "bash", "-c", fullCommand
        );

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            Process process = builder.start();

            int exitCode = process.waitFor(); // Wait for terminal to close
            JavonoLogger.info("ESP-IDF installation flow finished (or user closed the terminal). Exit code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            JavonoLogger.error("Failed to launch terminal for ESP-IDF install: " + e.getMessage());
        }
    }

    public static void installForMacOS() {
        String espDir = System.getProperty("user.home") + "/esp";

        // Compose the full shell script to run inside Terminal.app
        String commands = String.join(" && ", List.of(
                "echo 'Starting ESP-IDF install on macOS...'",
                // Step 1: Install prerequisites with Homebrew
                "brew update",
                "brew install cmake ninja dfu-util ccache",
                // Optional: check if python3 installed; if not, install
                "if ! command -v python3 &> /dev/null; then brew install python3; fi",
                // Step 2: Create esp dir and clone ESP-IDF
                "mkdir -p " + espDir,
                "cd " + espDir,
                "git clone -b v5.4.2 --recursive https://github.com/espressif/esp-idf.git",
                "cd esp-idf",
                // Step 3: Run install.sh for esp32 target
                "./install.sh esp32",
                // Step 4: Source export.sh (this will only affect this terminal session)
                "source export.sh",
                // Keep terminal open for user to see logs and interact
                "exec $SHELL"
        ));

        // AppleScript command to open Terminal.app and run the commands
        String appleScriptCommand =
                "tell application \"Terminal\"\n" +
                        "    activate\n" +
                        "    do script \"" + commands.replace("\"", "\\\"") + "\"\n" +
                        "end tell";

        // Run the AppleScript command via osascript
        List<String> cmd = List.of("osascript", "-e", appleScriptCommand);

        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.inheritIO();
            Process process = builder.start();
            int exitCode = process.waitFor();
            System.out.println("ESP-IDF installation terminal closed with exit code " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
