package javono.cli;

import javono.bootstrap.JavonoBootstrap;
import javono.builder.JavonoBuilder;
import javono.builder.impl.LocalBuilder;
import javono.builder.impl.RemoteBuilder;


public class JavonoCli {

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

        JavonoBuilder builder = useRemote ? new RemoteBuilder() : new LocalBuilder();

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

            case "help":
            default:
                printHelp();
                break;
        }
    }

    private static void printHelp() {
        System.out.println("Javono CLI - Commands:");
        System.out.println("     init              Set up the environment");
        System.out.println("     build [--remote]  Build the Java sketch (local by default)");
        System.out.println("     flash             Flash firmware to the device");
        System.out.println("     clean             Clean build artifacts");
        System.out.println("     help              Show this help message");
    }
}
