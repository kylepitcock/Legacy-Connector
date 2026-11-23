package org.kylepitcock.legacyconnector.core.degraded;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one channel for "the layer could not do this fully": an asset cap that cut a mod's models, a vanilla render type without a
 * handler, a GL query answered by the real context instead of the emulation, a mixin that did not apply, a legacy call that threw and
 * got a fallback value.
 *
 * <p>Each event is identified by (category, owner, key), where the owner is the mod (see {@link DegradedOwners}) or a class/subsystem
 * name. It is counted every time and logged once, at WARN, with the message of the first occurrence. The hot path is two
 * {@link ConcurrentHashMap} lookups and an atomic increment; the message is only built on the first occurrence when given as a supplier.
 * {@link #summaryLines} groups by owner and category; it is printed once the server has started and once when the client joins a
 * world ({@link #finish}) and also written to {@code logs/legacyconnector-degraded.txt}.
 *
 * <p>Bounded: at most {@link #MAX_KEYS_PER_OWNER} distinct keys are kept per owner and category (more only increment an overflow count),
 * and {@link #MAX_OWNERS} owners per category.
 */
public final class Degraded {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Degraded");

    public static final int MAX_KEYS_PER_OWNER = 200;
    public static final int MAX_OWNERS = 1024;
    public static final String OTHER = "(other)";
    public static final String FILE = "legacyconnector-degraded.txt";

    /** What kind of degradation; {@link #label} is the heading of the summary. */
    public enum Category {
        ASSET_CAP("asset generation cap hit"),
        ASSET_FALLBACK("asset fell back to a default"),
        RENDER_TYPE("block render type without a handler"),
        GL_EMULATION("GL call not emulated"),
        MIXIN("mixin not applied"),
        LEGACY_CALL("legacy call failed (fallback used)"),
        OTHER("other");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    /** One (category, owner, key): its first message and how often it happened. */
    public static final class Entry {
        public final String key;
        public final String message;
        final AtomicLong count = new AtomicLong();

        Entry(String key, String message) {
            this.key = key;
            this.message = message;
        }

        public long count() {
            return count.get();
        }
    }

    private static final class Cell {
        final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
        final AtomicLong overflow = new AtomicLong();
    }

    @SuppressWarnings("unchecked")
    private static final ConcurrentHashMap<String, Cell>[] BY_CATEGORY = new ConcurrentHashMap[Category.values().length];

    static {
        for (int i = 0; i < BY_CATEGORY.length; i++) BY_CATEGORY[i] = new ConcurrentHashMap<>();
    }

    private static volatile Path fileOverride;
    private static volatile boolean quiet;

    private Degraded() {}

    /** Notes an occurrence, logged at WARN the first time. Safe from any thread; never throws. */
    public static void note(Category category, String owner, String key, String message) {
        record(category, owner, key, () -> message, true);
    }

    /** As {@link #note(Category, String, String, String)}; the message is only built on the first occurrence. */
    public static void note(Category category, String owner, String key, Supplier<String> message) {
        record(category, owner, key, message, true);
    }

    /** As {@link #note} but logged at INFO the first time (approximations that are expected and usually harmless). */
    public static void info(Category category, String owner, String key, String message) {
        record(category, owner, key, () -> message, false);
    }

    /** As {@link #info(Category, String, String, String)} with a lazily built message. */
    public static void info(Category category, String owner, String key, Supplier<String> message) {
        record(category, owner, key, message, false);
    }

    private static void record(Category category, String owner, String key, Supplier<String> message, boolean warn) {
        try {
            if (category == null || key == null) return;
            if (owner == null) owner = OTHER;
            ConcurrentHashMap<String, Cell> owners = BY_CATEGORY[category.ordinal()];
            Cell cell = owners.get(owner);
            if (cell == null) {
                if (owners.size() >= MAX_OWNERS) owner = OTHER;
                cell = owners.computeIfAbsent(owner, k -> new Cell());
            }
            Entry e = cell.entries.get(key);
            if (e != null) {
                e.count.incrementAndGet();
                return;
            }
            if (cell.entries.size() >= MAX_KEYS_PER_OWNER) {
                cell.overflow.incrementAndGet();
                return;
            }
            String text = message == null ? "" : message.get();
            Entry fresh = new Entry(key, text == null ? "" : text);
            Entry prior = cell.entries.putIfAbsent(key, fresh);
            if (prior != null) {
                prior.count.incrementAndGet();
                return;
            }
            fresh.count.incrementAndGet();
            if (!quiet) {
                String tag = category.name().toLowerCase(java.util.Locale.ROOT);
                if (warn) LOG.warn("[{}] {}: {} ({})", tag, owner, fresh.message, key);
                else LOG.info("[{}] {}: {} ({})", tag, owner, fresh.message, key);
            }
        } catch (RuntimeException ignored) {
            // the reporting channel must never break the caller
        }
    }

    /** True when this exact event was noted before (cheap check to skip building an expensive message). */
    public static boolean seen(Category category, String owner, String key) {
        Cell cell = BY_CATEGORY[category.ordinal()].get(owner == null ? OTHER : owner);
        return cell != null && cell.entries.containsKey(key);
    }

    /** Distinct keys noted for the owner in the category. */
    public static int distinct(Category category, String owner) {
        Cell cell = BY_CATEGORY[category.ordinal()].get(owner);
        return cell == null ? 0 : cell.entries.size();
    }

    /** Occurrences of one event (0 when never noted). */
    public static long count(Category category, String owner, String key) {
        Cell cell = BY_CATEGORY[category.ordinal()].get(owner);
        Entry e = cell == null ? null : cell.entries.get(key);
        return e == null ? 0 : e.count();
    }

    /** Distinct events plus overflowed ones over everything. */
    public static long totalEvents() {
        long t = 0;
        for (var owners : BY_CATEGORY) for (Cell c : owners.values()) t += c.entries.size() + c.overflow.get();
        return t;
    }

    /** Clears everything and restores logging (tests). */
    public static void reset() {
        for (var owners : BY_CATEGORY) owners.clear();
        fileOverride = null;
        quiet = false;
    }

    /** Silences the per-event log line (tests). */
    public static void setQuiet(boolean q) {
        quiet = q;
    }

    // ------------------------------------------------------------------ output

    /**
     * The summary: a header, then one block per owner (sorted), under it one line per category with the number of distinct events and
     * the total occurrences, then up to {@code perCategory} keys (most frequent first) with their counts and first message. Empty
     * list when nothing was noted.
     */
    public static List<String> summaryLines(int perCategory) {
        TreeMap<String, TreeMap<Category, Cell>> byOwner = new TreeMap<>();
        for (Category c : Category.values()) {
            for (Map.Entry<String, Cell> e : BY_CATEGORY[c.ordinal()].entrySet()) {
                byOwner.computeIfAbsent(e.getKey(), k -> new TreeMap<>()).put(c, e.getValue());
            }
        }
        List<String> out = new ArrayList<>();
        if (byOwner.isEmpty()) return out;
        out.add("Degraded behaviour (written " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "): " + totalEvents() + " distinct event(s) in " + byOwner.size() + " place(s)");
        for (var owner : byOwner.entrySet()) {
            out.add("  " + owner.getKey());
            for (var cat : owner.getValue().entrySet()) {
                Cell cell = cat.getValue();
                long total = cell.overflow.get();
                for (Entry e : cell.entries.values()) total += e.count();
                out.add("    " + cat.getKey().label + ": " + (cell.entries.size() + cell.overflow.get()) + " distinct, " + total + " time(s)");
                List<Entry> sorted = new ArrayList<>(cell.entries.values());
                sorted.sort((a, b) -> Long.compare(b.count(), a.count()) != 0 ? Long.compare(b.count(), a.count()) : a.key.compareTo(b.key));
                int shown = 0;
                for (Entry e : sorted) {
                    if (shown++ >= perCategory) break;
                    out.add("      " + e.key + " x" + e.count() + (e.message.isEmpty() ? "" : ": " + oneLine(e.message)));
                }
                long more = (long) sorted.size() - Math.min(perCategory, sorted.size()) + cell.overflow.get();
                if (more > 0) out.add("      ...(+" + more + " more)");
            }
        }
        return out;
    }

    private static String oneLine(String s) {
        s = s.replace('\r', ' ').replace('\n', ' ').strip();
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    /** Where {@code logs/legacyconnector-degraded.txt} goes in tests; null restores the game directory. */
    public static void setFileForTests(Path p) {
        fileOverride = p;
    }

    private static Path file() {
        Path p = fileOverride;
        if (p != null) return p;
        try {
            return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("logs").resolve(FILE);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Logs the summary (INFO: header, then one line per owner and category) and rewrites the file. Never throws. */
    public static synchronized void finish(String phase) {
        try {
            List<String> lines = summaryLines(5);
            if (lines.isEmpty()) {
                LOG.info("Legacy Connector degraded behaviour ({}): none noted", phase);
            } else {
                LOG.info("Legacy Connector {} (at: {}); full list in logs/{}", lines.get(0), phase, FILE);
                for (String l : lines.subList(1, lines.size())) if (!l.startsWith("      ")) LOG.info("{}", l);
            }
            writeFile(phase);
        } catch (RuntimeException e) {
            LOG.debug("degraded summary failed: {}", e.toString());
        }
    }

    /** Rewrites the file with the full summary. Never throws. */
    public static synchronized void writeFile(String phase) {
        try {
            Path p = file();
            if (p == null) return;
            List<String> lines = summaryLines(Integer.MAX_VALUE / 2);
            if (lines.isEmpty()) lines = List.of("Degraded behaviour: none noted (" + phase + ")");
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.write(p, lines, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            LOG.debug("could not write {}: {}", FILE, e.toString());
        }
    }
}
