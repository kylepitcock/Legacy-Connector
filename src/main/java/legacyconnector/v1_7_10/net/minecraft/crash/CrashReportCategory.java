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

    /** A block position with its chunk and region, as crash reports show it. */
    public static String getLocationInfo(int x, int y, int z) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("World: (%d,%d,%d)", x, y, z)).append(", ");
        int cx = x >> 4, cz = z >> 4;
        sb.append(String.format("Chunk: (at %d,%d,%d in %d,%d; contains blocks %d,0,%d to %d,255,%d)", x & 15, y >> 4, z & 15, cx, cz,
                cx << 4, cz << 4, (cx + 1 << 4) - 1, (cz + 1 << 4) - 1)).append(", ");
        int rx = x >> 9, rz = z >> 9;
        sb.append(String.format("Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,0,%d to %d,255,%d)", rx, rz, rx << 5, rz << 5,
                (rx + 1 << 5) - 1, (rz + 1 << 5) - 1, rx << 9, rz << 9, (rx + 1 << 9) - 1, (rz + 1 << 9) - 1));
        return sb.toString();
    }

    /** An exact position followed by {@link #getLocationInfo} of its block. */
    public static String func_85074_a(double x, double y, double z) {
        return String.format("%.2f,%.2f,%.2f - %s", x, y, z, getLocationInfo((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
    }

    /** Adds the block's id and names, its metadata and its location to the section. */
    public static void addBlockInfo(CrashReportCategory category, int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.block.Block block, int meta) {
        int id = legacyconnector.v1_7_10.net.minecraft.block.Block.getIdFromBlock(block);
        category.addCrashSectionCallable("Block type", () -> {
            try {
                return String.format("ID #%d (%s // %s)", id, block.getUnlocalizedName(), block.getClass().getCanonicalName());
            } catch (Throwable t) {
                return "ID #" + id;
            }
        });
        category.addCrashSectionCallable("Block data value", () -> {
            if (meta < 0) return "Unknown? (Got " + meta + ")";
            String bits = String.format("%4s", Integer.toBinaryString(meta)).replace(" ", "0");
            return String.format("%1$d / 0x%1$X / 0b%2$s", meta, bits);
        });
        category.addCrashSectionCallable("Block location", () -> getLocationInfo(x, y, z));
    }

    public void appendToStringBuilder(StringBuilder sb) {
        appendTo(sb);
    }

    public String getName() {
        return name;
    }

    public void appendTo(StringBuilder sb) {
        sb.append("-- ").append(name).append(" --\n");
        sections.forEach((k, v) -> sb.append("\t").append(k).append(": ").append(v).append('\n'));
    }
}
