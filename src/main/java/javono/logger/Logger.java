package javono.logger;

class Logger {
    private static final String PREFIX = "[Javono] ";

    public void info(String message) {
        System.out.println(PREFIX + message);
    }

    public void warn(String message) {
        System.out.println(PREFIX + message);
    }

    public void error(String message) {
        System.err.println(PREFIX + message);
    }

    public void success(String message) {
        System.out.println(PREFIX + message);
    }
}
