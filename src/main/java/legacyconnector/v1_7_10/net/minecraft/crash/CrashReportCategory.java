package legacyconnector.v1_7_10.net.minecraft.crash;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** A named section of a crash report: key/value lines describing what the game was doing. */
public class CrashReportCategory {
    private final CrashReport report;
    private final String name;
    private final Map<String, String> sections = new LinkedHashMap<>();

    public CrashReportCategory(CrashReport report, String name) {
        this.report = report;
        this.name = name;
    }

    public void addCrashSection(String key, Object value) {
        sections.put(key, String.valueOf(value));
    }

    public void addCrashSectionCallable(String key, Callable<?> callable) {
        try {
            addCrashSection(key, callable.call());
        } catch (Exception e) {
            sections.put(key, "~~ERROR~~ " + e);
        }
    }

    public void addCrashSectionThrowable(String key, Throwable t) {
        addCrashSection(key, t);
    }

    public static String getCoordinateInfo(int x, int y, int z) {
        return "World: (" + x + "," + y + "," + z + ")";
    }

    public String getName() {
        return name;
    }

    public void appendTo(StringBuilder sb) {
        sb.append("-- ").append(name).append(" --\n");
        sections.forEach((k, v) -> sb.append("\t").append(k).append(": ").append(v).append('\n'));
    }
}
