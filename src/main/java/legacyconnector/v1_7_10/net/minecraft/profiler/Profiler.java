package legacyconnector.v1_7_10.net.minecraft.profiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7.10 profiler, reached through {@code World.theProfiler}, {@code MinecraftServer.theProfiler} and
 * {@code Minecraft.mcProfiler}. Never enabled: sections are accepted and ignored (the modern profiler is not fed).
 */
public class Profiler {
    /** Shared instance behind every legacy profiler field. */
    public static final Profiler INSTANCE = new Profiler();

    public final List sectionList = new ArrayList();
    public final List timestampList = new ArrayList();
    public boolean profilingEnabled;
    public String profilingSection = "";
    public final Map profilingMap = new HashMap();

    /** One line of the profiler's results: how much of its parent and of the whole tick a section took (1.7.10 {@code Profiler.Result}). */
    public static final class Result implements Comparable<Result> {
        public double field_76332_a;
        public double field_76330_b;
        public String field_76331_c;

        public Result(String name, double parentPercent, double totalPercent) {
            this.field_76331_c = name;
            this.field_76332_a = parentPercent;
            this.field_76330_b = totalPercent;
        }

        @Override
        public int compareTo(Result other) {
            if (other.field_76332_a < field_76332_a) return -1;
            return other.field_76332_a > field_76332_a ? 1 : other.field_76331_c.compareTo(field_76331_c);
        }

        /** The colour the profiler pie draws the section in, from its name. */
        public int func_76329_a() {
            return (field_76331_c.hashCode() & 11184810) + 4473924;
        }
    }

    public void clearProfiling() {
    }

    public void startSection(String name) {
    }

    public void endSection() {
    }

    public void endStartSection(String name) {
    }

    public List getProfilingData(String section) {
        return new ArrayList();
    }

    public String getNameOfLastSection() {
        return "";
    }
}
