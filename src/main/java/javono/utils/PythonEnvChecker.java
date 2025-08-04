package javono.utils;

import javono.installer.InstallerFacade;
import javono.logger.LoggerFacade;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class PythonEnvChecker {

    public static boolean isPythonCommandAvailable() {
        return runCommand("python3", "--version") == 0;
    }

    public static boolean isVenvAvailable() {
        return runCommand("python3", "-m", "venv", "--help") == 0;
    }

    public static boolean isEnsurePipAvailable() {
        return runCommand("python3", "-m", "ensurepip", "--help") == 0;
    }

    public static boolean isDebianBasedOS() {
        String result = captureOutput("cat", "/etc/os-release");
        return result.contains("Ubuntu") || result.contains("Debian");
    }

    private static String captureOutput(String... command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            return null;
        }
        return output.toString().trim();
    }

    private static int runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true) // combine stdout and stderr
                    .start();

            // Optionally consume output to avoid blocking
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // just consume output silently
                }
            }

            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            return -1; // indicate failure
        }
    }

    public void warnAndInstallIfMissing() {
        if (!isPythonCommandAvailable()) {
            LoggerFacade.getInstance().info("❌ Python3 is not installed.");
            try {
                LoggerFacade.getInstance().info("Installing Python");
                InstallerFacade.getInstance().ensureMinicondaInstalled();
                return;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        boolean venvOK = isVenvAvailable();
        boolean pipOK = isEnsurePipAvailable();

        if (venvOK && pipOK) {
            System.out.println("[Javono] Python venv and ensurepip are available.");
            return;
        }

        System.out.println("[Javono] ❌ Some required Python modules are missing:");
        if (!venvOK) System.out.println("  - venv");
        if (!pipOK) System.out.println("  - ensurepip");

        if (!isDebianBasedOS()) {
            System.out.println("[Javono] Unsupported OS detected. Please install missing modules manually.");
            return;
        }

        System.out.println("\n[Javono] You can fix this by running:");
        System.out.println("sudo apt install --reinstall python3-venv python3-ensurepip\n");

        System.out.print("[Javono] Would you like Javono to open a terminal to install these now? (y/N): ");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String input = reader.readLine();
            if (input != null && (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes"))) {
                try {
                    try {
                        UtilsFacade.getInstance().openSudoCommandInTerminal("sudo apt install python3-venv");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("[Javono] Terminal opened for installation. Please complete the install there.");
                } catch (IOException e) {
                    System.out.println("[Javono] Failed to open terminal for installation: " + e.getMessage());
                }
            } else {
                System.out.println("[Javono] Skipping automatic installation.");
            }
        } catch (IOException e) {
            System.out.println("[Javono] Error reading input, skipping automatic installation.");
        }
    }

    // Your existing runCommand and captureOutput methods here...

}
