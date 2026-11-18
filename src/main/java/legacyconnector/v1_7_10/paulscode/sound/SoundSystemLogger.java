package legacyconnector.v1_7_10.paulscode.sound;

/**
 * paulscode's log sink: the library and mods write messages through it, indented by level. Like the original it prints to the standard
 * streams; mods subclass it to redirect the lines (the three {@code print} methods are what they override).
 */
public class SoundSystemLogger {
    /** A plain message at the indent level (two spaces per level). */
    public void message(String message, int indent) {
        String spacer = indent(indent);
        print(spacer + message);
    }

    /** A message that matters, shown in any case. */
    public void importantMessage(String message, int indent) {
        String spacer = indent(indent);
        importantPrint(spacer + message);
    }

    /** Reports the failure when {@code error} is set (and returns it): {@code classname} names the reporting class. */
    public boolean errorCheck(boolean error, String classname, String message, int indent) {
        if (error) errorMessage(classname, message, indent);
        return error;
    }

    public void errorMessage(String classname, String message, int indent) {
        String spacer = indent(indent);
        errorPrint(spacer + "Error in class '" + classname + "'");
        errorPrint(spacer + "    " + message);
    }

    /** The exception's stack trace, at the indent level. */
    public void printStackTrace(Exception e, int indent) {
        if (e == null) return;
        String spacer = indent(indent);
        StackTraceElement[] trace = e.getStackTrace();
        if (trace == null) return;
        for (StackTraceElement element : trace) errorPrint(spacer + element.toString());
    }

    /** The exception's message and cause, at the indent level. */
    public void printExceptionMessage(Exception e, int indent) {
        if (e == null) return;
        String spacer = indent(indent);
        errorPrint(spacer + e.getMessage());
    }

    protected void print(String line) {
        System.out.println(line);
    }

    protected void importantPrint(String line) {
        System.out.println(line);
    }

    protected void errorPrint(String line) {
        System.err.println(line);
    }

    private static String indent(int level) {
        return "  ".repeat(Math.max(0, level));
    }
}
