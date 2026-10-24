package legacyconnector.v1_7_10.net.minecraft.crash;

import java.util.ArrayList;
import java.util.List;

/** The text of a crash. Legacy mods build these to report failures through {@code ReportedException}. */
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

    public CrashReportCategory makeCategory(String name) {
        return makeCategoryDepth(name, 1);
    }

    public CrashReportCategory makeCategoryDepth(String name, int stackDepth) {
        CrashReportCategory category = new CrashReportCategory(this, name);
        categories.add(category);
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Throwable getCrashCause() {
        return cause;
    }

    public String getCompleteReport() {
        StringBuilder sb = new StringBuilder("---- Minecraft Crash Report ----\n\nDescription: ").append(description).append("\n\n");
        if (cause != null) sb.append(cause).append('\n');
        for (CrashReportCategory c : categories) c.appendTo(sb);
        return sb.toString();
    }
}
