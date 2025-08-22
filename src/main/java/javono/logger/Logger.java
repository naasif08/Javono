package javono.logger;

class Logger {
    private static final String PREFIX = "[Javono] ";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    public void info(String message) {
        System.out.println(PREFIX + message);
    }

    public void warn(String message) {
        System.out.println(YELLOW + PREFIX + message + RESET);
    }

    public void error(String message) {
        System.err.println(RED + PREFIX + message + RESET);
    }

    public void success(String message) {
        System.out.println(PREFIX + message);
    }
}
