package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.crash.CrashReport;

/** A crash with its report; thrown up to the game's crash handler. */
public class ReportedException extends RuntimeException {
    private final CrashReport crashReport;

    public ReportedException(CrashReport report) {
        this.crashReport = report;
    }

    public CrashReport getCrashReport() {
        return crashReport;
    }

    @Override
    public Throwable getCause() {
        return crashReport.getCrashCause();
    }

    @Override
    public String getMessage() {
        return crashReport.getDescription();
    }
}
