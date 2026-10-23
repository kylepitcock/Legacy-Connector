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
