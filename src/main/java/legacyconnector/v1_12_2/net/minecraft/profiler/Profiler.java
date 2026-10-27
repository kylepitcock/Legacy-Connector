package legacyconnector.v1_12_2.net.minecraft.profiler;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.12.2 section profiler. Sections are only tracked by name (the modern game has its own profiler); timings are not
 * collected, so {@link #getProfilingData} is empty.
 */
public class Profiler {
    public boolean profilingEnabled;
    private final ThreadLocal<java.util.ArrayDeque<String>> sections = ThreadLocal.withInitial(java.util.ArrayDeque::new);

    public Profiler() {
    }

    public void clearProfiling() {
        sections.get().clear();
    }

    public void startSection(String name) {
        sections.get().push(name);
    }

    public void func_194340_a(java.util.function.Supplier<String> name) {
        startSection(name.get());
    }

    public void startSection(Class<?> type) {
        startSection(type.getSimpleName());
    }

    public void endSection() {
        var s = sections.get();
        if (!s.isEmpty()) s.pop();
    }

    public void endStartSection(String name) {
        endSection();
        startSection(name);
    }

    public void func_194339_b(java.util.function.Supplier<String> name) {
        endSection();
        func_194340_a(name);
    }

    public List<Object> getProfilingData(String name) {
        return new ArrayList<>();
    }

    public String getNameOfLastSection() {
        var s = sections.get();
        return s.isEmpty() ? "[UNKNOWN]" : s.peek();
    }
}
