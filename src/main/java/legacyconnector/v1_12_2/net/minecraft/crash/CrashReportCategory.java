package legacyconnector.v1_12_2.net.minecraft.crash;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** A named group of details in a crash report; the details are kept so mods can add to them, and printed when the report is. */
public class CrashReportCategory {
    private final CrashReport report;
    private final String name;
    private final Map<String, String> details = new LinkedHashMap<>();

    public CrashReportCategory(CrashReport report, String name) {
        this.report = report;
        this.name = name;
    }

    public static String getCoordinateInfo(BlockPos pos) {
        return pos == null ? "null" : "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
    }

    public static String getCoordinateInfo(double x, double y, double z) {
        return String.format("%.2f,%.2f,%.2f", x, y, z);
    }

    public static String getCoordinateInfo(int x, int y, int z) {
        return "(" + x + "," + y + "," + z + ")";
    }

    /** The block, its state and its location as crash details (vanilla's block section). */
    public static void addBlockInfo(CrashReportCategory category, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        if (state != null) {
            category.addDetail("Block", state::toString);
            var block = state.getBlock();
            if (block != null) {
                int id = legacyconnector.v1_12_2.net.minecraft.block.Block.getIdFromBlock(block);
                category.addDetail("Block type", () -> String.format("ID #%d (%s // %s)", id, block.getUnlocalizedName(), block.getClass().getName()));
            }
        }
        category.addDetail("Block location", () -> getCoordinateInfo(pos));
    }

    public void addCrashSection(String name, Object value) {
        details.put(name, String.valueOf(value));
    }

    public void addDetail(String name, ICrashReportDetail<String> detail) {
        try {
            details.put(name, detail.call());
        } catch (Throwable t) {
            details.put(name, "~~ERROR~~ " + t);
        }
    }

    public void addCrashSectionThrowable(String name, Throwable t) {
        details.put(name, String.valueOf(t));
    }

    public int getPrunedStackTrace(int size) {
        return 0;
    }

    public void appendToStringBuilder(StringBuilder b) {
        b.append("-- ").append(name).append(" --\nDetails:");
        for (Map.Entry<String, String> e : details.entrySet()) b.append("\n\t").append(e.getKey()).append(": ").append(e.getValue());
        b.append('\n');
    }

    public String getName() {
        return name;
    }
}
