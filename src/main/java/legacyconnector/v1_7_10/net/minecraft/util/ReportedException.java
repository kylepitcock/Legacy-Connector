package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.crash.CrashReport;

public class ReportedException extends RuntimeException {
    private final CrashReport report;

    public ReportedException(CrashReport report) {
        super(report.getDescription(), report.getCrashCause());
        this.report = report;
    }

    public CrashReport getCrashReport() {
        return report;
    }
}
