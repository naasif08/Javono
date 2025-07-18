package javono.utils;

import javono.logger.JavonoLogger;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class TerminalLauncher {

    /**
     * Open a terminal window, run a command (with optional sudo), and block until it's done.
     */
    public static void openSudoCommandInTerminal(String command) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            openSudoCommandInTerminalMac(command);
            return;
        } else if (os.contains("nux")) {
            openSudoCommandInTerminalLinux(command);
            return;
        } else {
            throw new UnsupportedOperationException("Unsupported OS for terminal launching.");
        }
    }

    /**
     * macOS: Launches Terminal and asks user to return when done (no true blocking possible).
     */
    private static void openSudoCommandInTerminalMac(String command) throws IOException {
        String appleScriptCommand = "osascript -e 'tell application \"Terminal\" to do script \"" + command + "\"'";
        new ProcessBuilder("bash", "-c", appleScriptCommand).start();

        System.out.println("🚀 Terminal opened. Please run the command, then close the terminal to continue.");
        System.out.println("🔁 Press ENTER here when you're done...");
        new Scanner(System.in).nextLine();
    }

    /**
     * Linux: Launch terminal and block until the task is done using a temp file flag.
     */
    private static void openSudoCommandInTerminalLinux(String command) throws IOException, InterruptedException {
        String doneFlagPath = "/tmp/juno_terminal_done";
        File doneFlag = new File(doneFlagPath);

        // Clean up any old flag
        if (doneFlag.exists()) {
            doneFlag.delete();
        }

        String terminalCommand = String.format(
                "gnome-terminal -- bash -c '%s; echo done > %s; echo; echo Press ENTER to close...; read'",
                command, doneFlagPath
        );

        new ProcessBuilder("bash", "-c", terminalCommand).start();

        JavonoLogger.info("⏳ Waiting for terminal task to complete...");
        while (!doneFlag.exists()) {
            Thread.sleep(1000);
        }

        doneFlag.delete();
        JavonoLogger.info("Terminal task completed.");
    }
}
