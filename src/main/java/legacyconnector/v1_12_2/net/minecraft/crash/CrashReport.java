package legacyconnector.v1_12_2.net.minecraft.crash;

import java.util.ArrayList;
import java.util.List;

/** A crash report: a description, the cause and the categories mods add details to. */
public class CrashReport {
    private final String description;
    private final Throwable cause;
    private final List<CrashReportCategory> categories = new ArrayList<>();

    public CrashReport(String description, Throwable cause) {
        this.description = description;
        this.cause = cause;
    }

    public static CrashReport makeCrashReport(Throwable cause, String description) {
        return new CrashReport(description, cause);
    }

    public String getDescription() {
        return description;
    }

    public Throwable getCrashCause() {
        return cause;
    }

    public CrashReportCategory makeCategory(String name) {
        CrashReportCategory c = new CrashReportCategory(this, name);
        categories.add(c);
        return c;
    }

    public CrashReportCategory makeCategoryDepth(String name, int stacktraceLength) {
        return makeCategory(name);
    }

    public CrashReportCategory getCategory() {
        return categories.isEmpty() ? makeCategory("Details") : categories.get(0);
    }

    public String getCompleteReport() {
        StringBuilder b = new StringBuilder("---- Minecraft Crash Report ----\n\nDescription: ").append(description).append("\n\n");
        for (CrashReportCategory c : categories) c.appendToStringBuilder(b);
        if (cause != null) b.append('\n').append(cause);
        return b.toString();
    }

    public String toString() {
        return getCompleteReport();
    }
}
