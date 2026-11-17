import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import jdk.jfr.consumer.*;

/**
 * Streaming JFR summary for the layer's own frames (the python jfr_report.py builds a JSON of the whole recording and does not finish on
 * recordings of a few hundred MB of events). Run: java tools/dev/perf/JfrLayer.java file.jfr [topN]
 * Per thread group (server, render, loading, worldgen, other): CPU samples (jdk.ExecutionSample) and allocation weight (jdk.ObjectAllocationSample) by
 *  - innermost layer frame (the layer method nearest the top of the stack = who in the layer burns it),
 *  - outermost layer frame (inclusive cost of a layer entry point) and the first frame outside the layer below it (who calls into the layer),
 * plus GC pauses, heap after GC, monitor contention and parks by layer frame.
 */
public final class JfrLayer {
    static final String[] LAYER = {"legacyconnector.", "org.kylepitcock.legacyconnector."};

    static boolean layer(String c) {
        for (String p : LAYER) if (c.startsWith(p)) return true;
        return false;
    }

    static String group(String t) {
        if (t == null) return "other";
        if (t.equals("Server thread")) return "server";
        if (t.equals("Render thread")) return "render";
        if (t.equals("main") || t.startsWith("modloading")) return "loading";
        if (t.startsWith("Worker-") || t.startsWith("ForkJoin") || t.contains("C2ME")) return "worker";
        return "other";
    }

    static String name(RecordedFrame f) {
        RecordedMethod m = f.getMethod();
        return m.getType().getName() + "." + m.getName();
    }

    static final class Agg {
        long total, layerOn;
        final Map<String, Long> self = new HashMap<>(), inner = new HashMap<>(), outer = new HashMap<>(), caller = new HashMap<>();
        final Map<String, Long> outerCaller = new HashMap<>(), layerSelf = new HashMap<>(), layerSelfCaller = new HashMap<>();
    }

    static final Map<String, Agg> CPU = new TreeMap<>(), ALLOC = new TreeMap<>();

    static void add(Map<String, Agg> m, String g, RecordedStackTrace st, long w) {
        for (String key : new String[] {g, "ALL"}) {
            Agg a = m.computeIfAbsent(key, k -> new Agg());
            a.total += w;
            if (st == null) continue;
            List<RecordedFrame> fr = st.getFrames();
            if (fr.isEmpty()) continue;
            a.self.merge(name(fr.get(0)), w, Long::sum);
            if (layer(fr.get(0).getMethod().getType().getName())) {
                a.layerSelf.merge(name(fr.get(0)), w, Long::sum);
                String cl = "(root)";
                for (int i = 1; i < fr.size(); i++) if (!name(fr.get(i)).equals(name(fr.get(0)))) { cl = name(fr.get(i)); break; }
                a.layerSelfCaller.merge(name(fr.get(0)) + "  <-  " + cl, w, Long::sum);
            }
            int first = -1, last = -1;
            for (int i = 0; i < fr.size(); i++) {
                if (layer(fr.get(i).getMethod().getType().getName())) {
                    if (first < 0) first = i;
                    last = i;
                }
            }
            if (first < 0) { a.inner.merge("(none)", w, Long::sum); continue; }
            a.layerOn += w;
            a.inner.merge(name(fr.get(first)), w, Long::sum);
            a.outer.merge(name(fr.get(last)), w, Long::sum);
            String c = last + 1 < fr.size() ? name(fr.get(last + 1)) : "(root)";
            a.outerCaller.merge(name(fr.get(last)) + "  <-  " + c, w, Long::sum);
        }
    }

    static void print(String title, Map<String, Agg> m, int top, String unit) {
        for (Map.Entry<String, Agg> e : m.entrySet()) {
            Agg a = e.getValue();
            if (a.total == 0) continue;
            System.out.printf("%n## %s / %s: %,d %s; a layer frame on %.1f%% of it%n", title, e.getKey(), a.total, unit, 100.0 * a.layerOn / a.total);
            section("top self frames", a.self, a.total, top);
            section("layer methods that are the top frame (own cost, no callee)", a.layerSelf, a.total, top);
            section("  ... with their direct caller", a.layerSelfCaller, a.total, top);
            section("innermost layer frame", a.inner, a.total, top);
            section("outermost layer frame (inclusive entry)", a.outer, a.total, top);
            section("entry <- caller outside the layer", a.outerCaller, a.total, top);
        }
    }

    static void section(String t, Map<String, Long> m, long total, int top) {
        System.out.println("  " + t + ":");
        m.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(top)
                .forEach(x -> System.out.printf("    %5.1f%%  %,14d  %s%n", 100.0 * x.getValue() / total, x.getValue(), x.getKey()));
    }

    public static void main(String[] args) throws Exception {
        int top = args.length > 1 ? Integer.parseInt(args[1]) : 15;
        Map<String, Long> allocClass = new HashMap<>(), mon = new HashMap<>(), park = new HashMap<>();
        long gcCount = 0, gcPauseNs = 0, gcMaxNs = 0, minHeap = Long.MAX_VALUE, maxHeap = 0, lastHeap = 0;
        double minT = Double.MAX_VALUE;
        long cpuSamples = 0;
        try (RecordingFile rf = new RecordingFile(Path.of(args[0]))) {
            while (rf.hasMoreEvents()) {
                RecordedEvent e = rf.readEvent();
                String t = e.getEventType().getName();
                switch (t) {
                    case "jdk.ExecutionSample": {
                        RecordedThread th = e.getThread("sampledThread");
                        add(CPU, group(th == null ? null : th.getJavaName()), e.getStackTrace(), 1);
                        cpuSamples++;
                        break;
                    }
                    case "jdk.ObjectAllocationSample": {
                        RecordedThread th = e.getThread("eventThread");
                        long w = e.getLong("weight");
                        add(ALLOC, group(th == null ? null : th.getJavaName()), e.getStackTrace(), w);
                        allocClass.merge(e.getClass("objectClass").getName(), w, Long::sum);
                        break;
                    }
                    case "jdk.GarbageCollection": {
                        gcCount++;
                        long p = e.getDuration("sumOfPauses").toNanos();
                        gcPauseNs += p;
                        gcMaxNs = Math.max(gcMaxNs, p);
                        break;
                    }
                    case "jdk.GCHeapSummary": {
                        if ("After GC".equals(e.getString("when"))) {
                            long u = e.getLong("heapUsed");
                            minHeap = Math.min(minHeap, u);
                            maxHeap = Math.max(maxHeap, u);
                            lastHeap = u;
                        }
                        break;
                    }
                    case "jdk.JavaMonitorEnter":
                    case "jdk.ThreadPark": {
                        RecordedStackTrace st = e.getStackTrace();
                        String key = "(no layer frame)";
                        if (st != null) for (RecordedFrame f : st.getFrames()) if (layer(f.getMethod().getType().getName())) { key = name(f); break; }
                        long ms = e.getDuration().toMillis();
                        (t.endsWith("Enter") ? mon : park).merge(key, ms, Long::sum);
                        break;
                    }
                    default:
                }
            }
        }
        System.out.printf("cpu samples %,d (20 ms profile interval => about %.0f s of sampled thread time)%n", cpuSamples, cpuSamples * 0.02);
        print("CPU samples", CPU, top, "samples");
        print("Allocation (sampled weight)", ALLOC, top, "bytes");
        System.out.println("\n## Top allocated classes (bytes)");
        allocClass.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(top)
                .forEach(x -> System.out.printf("  %,16d  %s%n", x.getValue(), x.getKey()));
        System.out.printf("%n## GC: %d collections, total pause %.0f ms, max pause %.0f ms; heap after GC min %d MB, max %d MB, last %d MB%n", gcCount,
                gcPauseNs / 1e6, gcMaxNs / 1e6, minHeap == Long.MAX_VALUE ? 0 : minHeap >> 20, maxHeap >> 20, lastHeap >> 20);
        for (Map.Entry<String, Map<String, Long>> e : Map.of("Monitor contention (ms) by layer frame", mon, "Thread park (ms) by layer frame", park).entrySet()) {
            System.out.println("\n## " + e.getKey());
            e.getValue().entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(top)
                    .forEach(x -> System.out.printf("  %,10d  %s%n", x.getValue(), x.getKey()));
        }
    }
}
