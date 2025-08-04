package javono.logger;


public class LoggerFacade {
    private static final LoggerFacade INSTANCE = new LoggerFacade();
    private static final Logger logger = new Logger();

    private LoggerFacade() {
    }

    public static LoggerFacade getInstance() {
        return INSTANCE;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void success(String message) {
        logger.success(message);
    }


}
