package javono.utils;



import javono.logger.LoggerFacade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Utility for running system shell commands.
 */
class CommandRunner {


    public static int runCommand(String... commandParts) throws IOException, InterruptedException {
        LoggerFacade.getInstance().info("Running: " + String.join(" ", commandParts));
        ProcessBuilder pb = new ProcessBuilder(commandParts);
        pb.inheritIO(); // optional: prints output directly
        Process process = pb.start();
        return process.waitFor();
    }

    /**
     * Runs the given command and blocks until it finishes.
     *
     * @param command array of command and args (e.g., ["git", "clone", "https://..."])
     * @return exit code of the process
     * @throws IOException if process cannot be started
     */
    public int runBlocking(String[] command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LoggerFacade.getInstance().info("[CMD] " + line);
            }
        }

        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /**
     * Runs the command and returns stdout output as a string (blocking).
     *
     * @param command array of command and args
     * @return full stdout text, or null if error
     */
    public static String runAndCaptureOutput(String[] command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return output.toString().trim();
    }

    /**
     * Logs and runs a command, mainly for debug.
     */
    public int runVerbose(String[] command) throws IOException {
        LoggerFacade.getInstance().info("[Running] " + Arrays.toString(command));
        return this.runBlocking(command);
    }
}
