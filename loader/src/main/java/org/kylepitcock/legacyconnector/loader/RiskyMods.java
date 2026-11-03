package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.kylepitcock.legacyconnector.loader.transform.LinkReport;

/**
 * The pre-launch summary of risky translated mods (one consolidated loading warning, dismissible once).
 *
 * <p>Risky = the link report found unresolved references (the mod throws when it uses them) or 10 or more synthesized
 * members (legacy API that links but does nothing); {@link #STUB_LIMIT} matches {@code STUB_LIMIT} in
 * {@code tools/compat/probe.py}. The per-jar numbers are stored next to the translation cache entry ({@code .risk.txt}) so
 * a cache hit still knows them. The warning carries a hash of the sorted (mod id, version, kind) set; once it was shown
 * the hash is recorded in {@code .legacyconnector/acknowledged-risks.txt} and the same set is silent from then on.</p>
 */
final class RiskyMods {
    static final int STUB_LIMIT = 10;
    static final int LISTED = 12;
    private static final String STATE_FILE = "acknowledged-risks.txt";

    /** One mod's (or jar's) risk. {@code kind} is {@code unresolved} or {@code stubs}. */
    record Entry(String id, String version, String kind, int count, List<String> symbols) {}

    /** What the link report of one jar says; {@code null} when the jar is not risky. */
    record Summary(int errors, int stubs, List<String> symbols) {}

    private RiskyMods() {}

    static Summary summarize(LinkReport links) {
        int errors = links.errorCount();
        int stubs = links.stubbedMembers.size();
        if (errors == 0 && stubs < STUB_LIMIT) return null;
        List<String> symbols = new ArrayList<>();
        if (errors > 0) {
            for (Map<String, ?> m : List.of(links.missingClasses, links.missingShimMembers, links.missingLibraryMembers)) {
                for (String k : m.keySet()) if (symbols.size() < 2) symbols.add(shorten(k));
            }
        } else {
            for (String k : links.stubbedMembers.keySet()) if (symbols.size() < 2) symbols.add(shorten(k));
        }
        return new Summary(errors, stubs, symbols);
    }

    /** {@code a/b/C.m(Ljava/lang/String;)V} -> {@code a.b.C.m(...)}; fields lose their type; the shim's package prefix is dropped. */
    static String shorten(String symbol) {
        String s = symbol.replace('/', '.');
        int paren = s.indexOf('(');
        if (paren >= 0) s = s.substring(0, paren) + "(...)";
        else if (s.indexOf(':') > 0) s = s.substring(0, s.indexOf(':'));
        return s.replaceFirst("^legacyconnector\\.v1_(7_10|12_2)\\.", "");
    }

    static void write(Path file, Summary s) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (s != null) {
            sb.append("errors=").append(s.errors()).append('\n').append("stubs=").append(s.stubs()).append('\n');
            for (String sym : s.symbols()) sb.append("sym=").append(sym).append('\n');
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Reads a {@code .risk.txt}; {@code null} when absent, empty or unreadable (never fails). */
    static Summary read(Path file) {
        try {
            if (!Files.isRegularFile(file)) return null;
            int errors = 0, stubs = 0;
            List<String> symbols = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.startsWith("errors=")) errors = Integer.parseInt(line.substring(7).trim());
                else if (line.startsWith("stubs=")) stubs = Integer.parseInt(line.substring(6).trim());
                else if (line.startsWith("sym=")) symbols.add(line.substring(4));
            }
            return errors == 0 && stubs < STUB_LIMIT ? null : new Summary(errors, stubs, symbols);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static Entry entry(LegacyJar jar, Summary s) {
        String id = jar.fileName();
        String version = "";
        if (!jar.mods.isEmpty()) {
            LegacyModDecl d = jar.mods.get(0);
            if (d.legacyId != null && !d.legacyId.isBlank()) id = d.legacyId;
            if (d.version != null) version = d.version;
        }
        return s.errors() > 0 ? new Entry(id, version, "unresolved", s.errors(), s.symbols())
                : new Entry(id, version, "stubs", s.stubs(), s.symbols());
    }

    /** SHA-256 over the sorted (mod id, version, kind) lines; counts and symbols are deliberately not part of it. */
    static String hash(List<Entry> entries) {
        TreeSet<String> lines = new TreeSet<>();
        for (Entry e : entries) lines.add(e.id() + "\t" + e.version() + "\t" + e.kind());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest()).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The warning text (no quotes or braces: FML formats it as a message); {@code null} when there is nothing to show. */
    static String message(List<Entry> entries) {
        if (entries.isEmpty()) return null;
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt((Entry e) -> "unresolved".equals(e.kind()) ? 0 : 1)
                .thenComparing(Comparator.comparingInt(Entry::count).reversed()).thenComparing(Entry::id));
        StringBuilder sb = new StringBuilder("Legacy Connector: ").append(sorted.size()).append(" translated mod(s) may not work fully "
                + "(unresolved = will throw when it uses that API, stubs = API that links but does nothing). "
                + "Shown once for this set of mods; disable with warnings.riskyMods = false.");
        int shown = 0;
        for (Entry e : sorted) {
            if (shown++ == LISTED) break;
            sb.append("\n- ").append(e.id());
            if (!e.version().isEmpty()) sb.append(' ').append(e.version());
            sb.append(": ").append(e.count()).append("unresolved".equals(e.kind()) ? " unresolved reference(s)" : " stubbed API member(s)");
            if (!e.symbols().isEmpty()) sb.append(", e.g. ").append(String.join(", ", e.symbols()));
        }
        if (sorted.size() > LISTED) sb.append("\n...and ").append(sorted.size() - LISTED).append(" more, see logs/legacyconnector-report.txt");
        else sb.append("\nDetails: logs/legacyconnector-report.txt");
        return sb.toString().replace("'", "").replace("{", "(").replace("}", ")");
    }

    static boolean acknowledged(Path stateDir, String hash) {
        try {
            Path f = stateDir.resolve(STATE_FILE);
            return Files.isRegularFile(f) && Files.readAllLines(f, StandardCharsets.UTF_8).contains(hash);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Remembers the hash (the newest 50 are kept). Failure to write only means the warning shows again. */
    static void acknowledge(Path stateDir, String hash) {
        try {
            Path f = stateDir.resolve(STATE_FILE);
            Set<String> all = new LinkedHashSet<>();
            if (Files.isRegularFile(f)) all.addAll(Files.readAllLines(f, StandardCharsets.UTF_8));
            all.remove(hash);
            all.add(hash);
            List<String> list = new ArrayList<>(all);
            if (list.size() > 50) list = list.subList(list.size() - 50, list.size());
            Files.createDirectories(stateDir);
            Files.write(f, list, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) {
            // the warning simply comes back on the next start
        }
    }
}
