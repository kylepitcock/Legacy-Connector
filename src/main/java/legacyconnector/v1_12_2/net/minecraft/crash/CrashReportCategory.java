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

    public static void addBlockInfo(CrashReportCategory category, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.Block block, int blockData) {
        int id = legacyconnector.v1_12_2.net.minecraft.block.Block.getIdFromBlock(block);
        category.addDetail("Block type", () -> {
            try {
                return String.format("ID #%d (%s // %s // %s)", id, block.getTranslationKey(), block.getClass().getName(), block.getRegistryName());
            } catch (Throwable t) {
                return "ID #" + id;
            }
        });
        category.addDetail("Block data value", () -> {
            if (blockData < 0) return "Unknown? (Got " + blockData + ")";
            String bits = String.format("%4s", Integer.toBinaryString(blockData)).replace(' ', '0');
            return String.format("%1$d / 0x%1$X / 0b%2$s", blockData, bits);
        });
        category.addDetail("Block location", () -> getCoordinateInfo(pos));
    }

    public static void addBlockInfo(CrashReportCategory category, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        category.addDetail("Block", () -> String.valueOf(state));
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
