package javono.cli;

import javono.bootstrap.JavonoBootstrap;
import javono.builder.JavonoBuilder;
import javono.builder.impl.JavonoLocalBuilder;
import javono.builder.impl.RemoteBuilder;
import javono.logger.LoggerFacade;


public class JavonoCli {

    public static final String JAVONO_VERSION = "1.0.3";

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0];
        boolean useRemote = false;

        // Parse optional flags like --remote
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--remote")) {
                useRemote = true;
            }
        }

        JavonoBuilder builder = useRemote ? new RemoteBuilder() : new JavonoLocalBuilder();

        switch (command) {
            case "init":
                JavonoBootstrap.setEnvironmentForTheFirstTime();
                break;

            case "build":
                builder.build();
                break;

            case "flash":
                builder.flash();
                break;

            case "clean":
                builder.clean();
                break;

            case "version":
            case "--version":
                LoggerFacade.getInstance().info("Javono CLI version: " + JAVONO_VERSION);
                break;

            case "uninstall":
                JavonoBootstrap.uninstallJavono();
                break;

            case "help":
            default:
                printHelp();
                break;
        }
    }

    private static void printHelp() {
        LoggerFacade.getInstance().info("Javono CLI - Commands:");
        LoggerFacade.getInstance().info("     init              Set up the environment");
        LoggerFacade.getInstance().info("     build [--remote]  Build the Java sketch (local by default)");
        LoggerFacade.getInstance().info("     flash             Flash firmware to the device");
        LoggerFacade.getInstance().info("     clean             Clean build artifacts");
        LoggerFacade.getInstance().info("    --version          Shows current version of Javono");
        LoggerFacade.getInstance().info("    uninstall          This will uninstall the Javono");
        LoggerFacade.getInstance().info("     help              Show this help message");

    }
}
