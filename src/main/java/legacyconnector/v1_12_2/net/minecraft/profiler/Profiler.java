package legacyconnector.v1_12_2.net.minecraft.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1.12's section profiler. Mods push and pop sections around their work; it records nothing until {@link #profilingEnabled} is
 * set (as in vanilla, where only the debug screen and /debug turned it on). It is kept apart from the modern profiler, so a mod
 * that pops more sections than it pushed cannot unbalance the game's own.
 */
public class Profiler {
    private final List<String> sectionList = new ArrayList<>();
    private final List<Long> timestampList = new ArrayList<>();
    public boolean profilingEnabled;
    private String profilingSection = "";
    private final Map<String, Long> profilingMap = new HashMap<>();

    public Profiler() {
    }

    public void clearProfiling() {
        profilingMap.clear();
        profilingSection = "";
        sectionList.clear();
    }

    public void startSection(String name) {
        if (!profilingEnabled) return;
        if (!profilingSection.isEmpty()) profilingSection = profilingSection + ".";
        profilingSection = profilingSection + name;
        sectionList.add(profilingSection);
        timestampList.add(System.nanoTime());
    }

    public void func_194340_a(Supplier<String> nameSupplier) {
        if (profilingEnabled) startSection(nameSupplier.get());
    }

    public void startSection(Class<?> type) {
        if (profilingEnabled) startSection(type.getSimpleName());
    }

    public void endSection() {
        if (!profilingEnabled || timestampList.isEmpty()) return;
        long now = System.nanoTime();
        long start = timestampList.remove(timestampList.size() - 1);
        sectionList.remove(sectionList.size() - 1);
        profilingMap.merge(profilingSection, now - start, Long::sum);
        profilingSection = sectionList.isEmpty() ? "" : sectionList.get(sectionList.size() - 1);
    }

    /** The share of {@code profilerName}'s time each of its direct subsections took (and the rest as "unspecified"). */
    public List<Result> getProfilingData(String profilerName) {
        if (!profilingEnabled) return Collections.emptyList();
        long root = profilingMap.getOrDefault("root", 0L);
        long total = profilingMap.getOrDefault(profilerName, -1L);
        List<Result> out = new ArrayList<>();
        String prefix = profilerName.isEmpty() ? "" : profilerName + ".";
        long sum = 0L;
        for (String s : profilingMap.keySet()) {
            if (s.length() > prefix.length() && s.startsWith(prefix) && s.indexOf('.', prefix.length() + 1) < 0) sum += profilingMap.get(s);
        }
        float f = (float) sum;
        if (sum < total) sum = total;
        if (root < sum) root = sum;
        for (String s : profilingMap.keySet()) {
            if (s.length() > prefix.length() && s.startsWith(prefix) && s.indexOf('.', prefix.length() + 1) < 0) {
                long l = profilingMap.get(s);
                out.add(new Result(s.substring(prefix.length()), (double) l * 100.0D / (double) sum, (double) l * 100.0D / (double) root));
            }
        }
        if ((float) sum > f) out.add(new Result("unspecified", (double) ((float) sum - f) * 100.0D / (double) sum, (double) ((float) sum - f) * 100.0D / (double) root));
        Collections.sort(out);
        out.add(0, new Result(profilerName, 100.0D, (double) sum * 100.0D / (double) Math.max(1L, root)));
        return out;
    }

    public void endStartSection(String name) {
        endSection();
        startSection(name);
    }

    public void func_194339_b(Supplier<String> nameSupplier) {
        endSection();
        func_194340_a(nameSupplier);
    }

    public String getNameOfLastSection() {
        return sectionList.isEmpty() ? "[UNKNOWN]" : sectionList.get(sectionList.size() - 1);
    }

    public static final class Result implements Comparable<Result> {
        public double usePercentage;
        public double totalUsePercentage;
        public String profilerName;

        public Result(String profilerName, double usePercentage, double totalUsePercentage) {
            this.profilerName = profilerName;
            this.usePercentage = usePercentage;
            this.totalUsePercentage = totalUsePercentage;
        }

        @Override
        public int compareTo(Result other) {
            if (other.usePercentage < usePercentage) return -1;
            return other.usePercentage > usePercentage ? 1 : other.profilerName.compareTo(profilerName);
        }

        public int getColor() {
            return (profilerName.hashCode() & 11184810) + 4473924;
        }
    }
}
