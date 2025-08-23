package javono.utils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * DiagnosticListener that prints compiler messages in color:
 * - Red for errors
 * - Yellow for warnings
 * - White for notes
 */
class ColoredDiagnosticListener implements DiagnosticListener<JavaFileObject> {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String WHITE = "\u001B[37m";

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (diagnostic == null) return;

        String source = diagnostic.getSource() != null ? diagnostic.getSource().getName() : "Unknown source";
        String message = String.format(
                "%s:%d:%d: %s: %s",
                source,
                diagnostic.getLineNumber(),
                diagnostic.getColumnNumber(),
                diagnostic.getKind(),
                diagnostic.getMessage(null)
        );

        switch (diagnostic.getKind()) {
            case ERROR:
                System.err.println(RED + message + RESET);
                break;
            case WARNING:
            case MANDATORY_WARNING:
                System.out.println(YELLOW + message + RESET);
                break;
            case NOTE:
                System.out.println(WHITE + message + RESET);
                break;
            default:
                System.out.println(message);
        }
    }
}