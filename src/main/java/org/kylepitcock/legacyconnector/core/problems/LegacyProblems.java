package org.kylepitcock.legacyconnector.core.problems;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The runtime problem collector shared by every legacy version: broken content (sprites that resolved to the missing texture, models that
 * fell back to the missing model, items and blocks without a model or a display name, recipes dropped because they name a missing item)
 * and a per-mod census of what was registered against what has a model or renderer. Mods are told apart by registry namespace.
 *
 * <p>Thread safe (Sodium and C2ME threads may record), O(1) per record, bounded memory: each (mod, kind) keeps at most {@link #CAP}
 * distinct subjects, every further new one only increments an overflow counter, and at most {@link #MAX_MODS} namespaces are tracked.
 * Nothing is logged per record: {@link #logSummary} writes one line to latest.log and {@link #writeReport} one section (marker line
 * {@link #SECTION}, last in the file) into {@code logs/legacyconnector-report.txt}. The format is parsed by tools/compat/from_client.py:
 * <pre>
 * Runtime problems (written 2026-11-16T12:00:00; client and server checks of this session)
 *   census ns: blocks=12 blocks_with_model=12 items=30 ...
 *   problems ns: missing_texture=2 missing_model=0 ... total=2
 *     missing_texture legacy:foo#inventory: quad uses mod:block/x
 *     ...(+N more)
 * total: 2 problem(s) in 1 mod(s)
 * </pre>
 */
public final class LegacyProblems {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Problems");

    /** First line of the report section. */
    public static final String SECTION = "Runtime problems";
    /** Distinct subjects kept per mod and kind. */
    public static final int CAP = 40;
    /** Namespaces tracked; later ones are counted under {@link #OTHER}. */
    public static final int MAX_MODS = 512;
    public static final String OTHER = "(other)";

    /** The problem kinds; {@link #key} is the stable token of the report and the command. */
    public enum Kind {
        MISSING_TEXTURE("missing_texture", "missing texture"),
        MISSING_MODEL("missing_model", "missing model"),
        NO_ITEM_MODEL("no_item_model", "item without a model"),
        NO_BLOCK_MODEL("no_block_model", "block without a model"),
        NO_LANG_NAME("no_lang_name", "no display name"),
        DROPPED_RECIPE("dropped_recipe", "recipe dropped");

        public final String key;
        public final String label;

        Kind(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    /** One mod's records: per kind a bounded map subject to detail, plus the count of unseen subjects past the cap. */
    private static final class Bucket {
        final ConcurrentHashMap<String, String>[] seen;
        final AtomicLong[] overflow = new AtomicLong[Kind.values().length];
        final ConcurrentHashMap<String, Long> census = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        Bucket() {
            seen = new ConcurrentHashMap[Kind.values().length];
            for (int i = 0; i < seen.length; i++) {
                seen[i] = new ConcurrentHashMap<>();
                overflow[i] = new AtomicLong();
            }
        }

        long count(Kind k) {
            return seen[k.ordinal()].size() + overflow[k.ordinal()].get();
        }
    }

    private static final String NL = String.valueOf((char) 10);
    private static final ConcurrentHashMap<String, Bucket> BUCKETS = new ConcurrentHashMap<>();
    private static final Set<String> EXTRA_NAMESPACES = ConcurrentHashMap.newKeySet();
    private static volatile long lastLoggedTotal = -1;
    private static boolean hookInstalled;

    private LegacyProblems() {}

    // ------------------------------------------------------------------ registration

    /** Common-side hooks: the registry census once a server has started, and the report section when the JVM exits. */
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            registryCensus();
            if (FMLEnvironment.dist.isDedicatedServer()) finish("server started");
        });
        ProblemsCommand.register();
        synchronized (LegacyProblems.class) {
            if (!hookInstalled) {
                hookInstalled = true;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> writeReport(), "LegacyConnector-problems"));
            }
        }
        if (FMLEnvironment.dist.isClient()) ProblemsClient.register(modBus);
    }

    /** Marks a namespace as legacy content (for mods the layer's registry does not list, e.g. translated 1.20.1 jars). */
    public static void markLegacyNamespace(String namespace) {
        if (namespace != null) EXTRA_NAMESPACES.add(namespace);
    }

    /** True for the registry namespace of a legacy mod (never for vanilla or modern mods). */
    public static boolean isLegacyNamespace(String namespace) {
        if (namespace == null || namespace.equals("minecraft")) return false;
        if (EXTRA_NAMESPACES.contains(namespace)) return true;
        for (LegacyModRecord m : LegacyMods.all()) {
            if (namespace.equalsIgnoreCase(m.modernId) || namespace.equalsIgnoreCase(m.legacyId)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ recording

    private static Bucket bucket(String namespace) {
        Bucket b = BUCKETS.get(namespace);
        if (b != null) return b;
        if (BUCKETS.size() >= MAX_MODS) namespace = OTHER;
        return BUCKETS.computeIfAbsent(namespace, k -> new Bucket());
    }

    /** Records one problem; the same subject of the same kind and mod counts once. Safe from any thread. */
    public static void record(Kind kind, String namespace, String subject, String detail) {
        if (kind == null || namespace == null || subject == null) return;
        Bucket b = bucket(namespace);
        var map = b.seen[kind.ordinal()];
        if (map.containsKey(subject)) return;
        if (map.size() >= CAP) {
            b.overflow[kind.ordinal()].incrementAndGet();
            return;
        }
        map.putIfAbsent(subject, clean(detail));
    }

    /** Records a problem of the content with this registry id; the namespace is the mod. */
    public static void record(Kind kind, net.minecraft.resources.ResourceLocation id, String detail) {
        if (id != null) record(kind, id.getNamespace(), id.toString(), detail);
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.replace('\r', ' ').replace('\n', ' ').strip();
        return s.length() > 160 ? s.substring(0, 160) : s;
    }

    /** Sets a census value of a mod (blocks, blocks_with_model, ...). */
    public static void census(String namespace, String key, long value) {
        if (namespace != null) bucket(namespace).census.put(key, value);
    }

    /** Adds to a census value of a mod. */
    public static void censusAdd(String namespace, String key, long delta) {
        if (namespace != null) bucket(namespace).census.merge(key, delta, Long::sum);
    }

    /** Count of problems of this kind recorded for the namespace (stored and overflowed). */
    public static long count(String namespace, Kind kind) {
        Bucket b = BUCKETS.get(namespace);
        return b == null ? 0 : b.count(kind);
    }

    public static long total() {
        long t = 0;
        for (Bucket b : BUCKETS.values()) for (Kind k : Kind.values()) t += b.count(k);
        return t;
    }

    /** Clears everything (tests). */
    public static void reset() {
        BUCKETS.clear();
        EXTRA_NAMESPACES.clear();
        lastLoggedTotal = -1;
    }

    // ------------------------------------------------------------------ census of the registries (server side, no client classes)

    private static final String[] COUNTED = {"blocks", "items", "entities", "block_entities"};

    /** Counts the registered blocks, items, entity types and block entity types of every legacy mod. */
    public static void registryCensus() {
        try {
            Map<String, long[]> counts = new TreeMap<>();
            countRegistry(BuiltInRegistries.BLOCK, counts, 0);
            countRegistry(BuiltInRegistries.ITEM, counts, 1);
            countRegistry(BuiltInRegistries.ENTITY_TYPE, counts, 2);
            countRegistry(BuiltInRegistries.BLOCK_ENTITY_TYPE, counts, 3);
            for (var e : counts.entrySet()) {
                for (int i = 0; i < COUNTED.length; i++) census(e.getKey(), COUNTED[i], e.getValue()[i]);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("registry census failed: {}", e.toString());
        }
    }

    private static void countRegistry(net.minecraft.core.Registry<?> registry, Map<String, long[]> counts, int slot) {
        for (var key : registry.keySet()) {
            String ns = key.getNamespace();
            if (!isLegacyNamespace(ns)) continue;
            counts.computeIfAbsent(ns, k -> new long[COUNTED.length])[slot]++;
        }
    }

    // ------------------------------------------------------------------ output

    /** The report section, one string per line (no trailing newline). */
    public static List<String> reportLines() {
        List<String> out = new ArrayList<>();
        out.add(SECTION + " (written " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "; client checks only appear in a client session)");
        TreeMap<String, Bucket> sorted = new TreeMap<>(BUCKETS);
        for (var e : sorted.entrySet()) {
            if (e.getValue().census.isEmpty()) continue;
            StringBuilder sb = new StringBuilder("  census ").append(e.getKey()).append(':');
            new TreeMap<>(e.getValue().census).forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
            out.add(sb.toString());
        }
        long grandTotal = 0;
        int mods = 0;
        for (var e : sorted.entrySet()) {
            Bucket b = e.getValue();
            long total = 0;
            for (Kind k : Kind.values()) total += b.count(k);
            if (total == 0) continue;
            mods++;
            grandTotal += total;
            StringBuilder sb = new StringBuilder("  problems ").append(e.getKey()).append(':');
            for (Kind k : Kind.values()) sb.append(' ').append(k.key).append('=').append(b.count(k));
            sb.append(" total=").append(total);
            out.add(sb.toString());
            for (Kind k : Kind.values()) {
                var seen = b.seen[k.ordinal()];
                if (seen.isEmpty()) continue;
                for (String subject : new TreeSet<>(seen.keySet())) {
                    String d = seen.get(subject);
                    out.add("    " + k.key + " " + subject + (d == null || d.isEmpty() ? "" : ": " + d));
                }
                long more = b.overflow[k.ordinal()].get();
                if (more > 0) out.add("    " + k.key + " ...(+" + more + " more)");
            }
        }
        out.add("total: " + grandTotal + " problem(s) in " + mods + " mod(s)");
        return out;
    }

    /** Short lines for the chat: one per mod with problems, worst first, then where the report is. */
    public static List<String> chatLines(int maxMods) {
        record Row(String ns, long total, String text) {}
        List<Row> rows = new ArrayList<>();
        for (var e : BUCKETS.entrySet()) {
            Bucket b = e.getValue();
            long total = 0;
            StringBuilder sb = new StringBuilder();
            for (Kind k : Kind.values()) {
                long c = b.count(k);
                if (c == 0) continue;
                total += c;
                sb.append(' ').append(c).append(' ').append(k.label).append(';');
            }
            if (total > 0) rows.add(new Row(e.getKey(), total, sb.toString()));
        }
        rows.sort((a, c) -> Long.compare(c.total, a.total) != 0 ? Long.compare(c.total, a.total) : a.ns.compareTo(c.ns));
        List<String> out = new ArrayList<>();
        long all = 0;
        for (Row r : rows) all += r.total;
        out.add(all == 0 ? "Legacy Connector: no problems recorded so far." : "Legacy Connector: " + all + " problem(s) in " + rows.size() + " mod(s):");
        for (int i = 0; i < rows.size() && i < maxMods; i++) out.add("  " + rows.get(i).ns + ":" + rows.get(i).text);
        if (rows.size() > maxMods) out.add("  ...and " + (rows.size() - maxMods) + " more mod(s)");
        out.add("Full list: logs/legacyconnector-report.txt (section \"" + SECTION + "\") in the game folder");
        return out;
    }

    public static Path reportPath() {
        return FMLPaths.GAMEDIR.get().resolve("logs").resolve("legacyconnector-report.txt");
    }

    /** One summary line in latest.log, only when the total changed since the last one. */
    public static void logSummary(String phase) {
        long t = total();
        if (t == lastLoggedTotal) return;
        lastLoggedTotal = t;
        int mods = 0;
        for (Bucket b : BUCKETS.values()) {
            for (Kind k : Kind.values()) if (b.count(k) > 0) { mods++; break; }
        }
        LOG.info("Legacy Connector problems ({}): {} in {} mod(s); details in logs/legacyconnector-report.txt, or /legacyconnector problems", phase, t, mods);
    }

    /** Logs the summary line and rewrites the report section. */
    public static void finish(String phase) {
        logSummary(phase);
        writeReport();
    }

    /** Replaces the section (from its marker line to the end) of the report, or appends it. Never throws. */
    public static synchronized void writeReport() {
        try {
            Path report = reportPath();
            if (!Files.isRegularFile(report)) return; // the loader writes the file first; nothing to extend without it
            String text = Files.readString(report, StandardCharsets.UTF_8);
            text = stripSection(text);
            StringBuilder sb = new StringBuilder(text);
            if (!text.endsWith("\n")) sb.append('\n');
            sb.append('\n');
            for (String line : reportLines()) sb.append(line).append('\n');
            Files.writeString(report, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LOG.debug("could not write the problems section: {}", e.toString());
        }
    }

    /** The report text without an earlier problems section (it starts at a line beginning with the marker and runs to the end). */
    static String stripSection(String text) {
        int i = text.startsWith(SECTION) ? 0 : text.indexOf(NL + SECTION);
        if (i < 0) return text;
        String head = text.substring(0, i).stripTrailing();
        return head.isEmpty() ? "" : head + NL;
    }
}
