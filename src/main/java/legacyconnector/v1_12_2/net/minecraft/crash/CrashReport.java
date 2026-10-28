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

    // ---- area F

    private java.io.File mcc$file;

    public java.io.File getFile() {
        return mcc$file;
    }

    /** Writes the complete report to the file (its folder is created); false when it could not be written. */
    public boolean saveToFile(java.io.File toFile) {
        if (mcc$file != null) return false;
        try {
            if (toFile.getParentFile() != null) toFile.getParentFile().mkdirs();
            java.nio.file.Files.writeString(toFile.toPath(), getCompleteReport(), java.nio.charset.StandardCharsets.UTF_8);
            mcc$file = toFile;
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").error("Could not save crash report to {}", toFile, e);
            return false;
        }
    }
}
