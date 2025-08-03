package javono.logger;
public class Logger {
    private static final String PREFIX = "[Javono] ";

    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    public static void warn(String message) {
        System.out.println(PREFIX + message);
    }

    public static void error(String message) {
        System.out.println(PREFIX + message);
    }

    public static void success(String message) {
        System.out.println(PREFIX + message);
    }
}
