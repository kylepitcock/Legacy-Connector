package org.kylepitcock.legacyconnector.loader.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * The declared members of every Minecraft 1.21.1 and NeoForge class ({@code mc-members.index.gz}, made by
 * {@code tools.McMembers1201Tool}): the knowledge {@link SoftLink1201} needs to tell which references of a Forge 1.20.1 mod cannot link.
 * Held softly: nothing reads it once the jars are translated.
 */
public final class McMembers1201 {
    /** Declared methods ({@code name+desc}) and fields ({@code name:desc}) of one class. */
    public record Members(Set<String> methods, Set<String> fields) {}

    private static SoftReference<Map<String, String>> lines = new SoftReference<>(null);
    private static final Map<String, Members> PARSED = new HashMap<>();
    private static Map<String, Members> testOverride;

    private McMembers1201() {}

    private static synchronized Map<String, String> lines() {
        Map<String, String> m = lines.get();
        if (m != null) return m;
        m = new HashMap<>(16384);
        try (InputStream raw = McMembers1201.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/mc-members.index.gz")) {
            if (raw != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8));
                for (String l = r.readLine(); l != null; l = r.readLine()) {
                    int tab = l.indexOf('\t');
                    if (tab > 0) m.put(l.substring(0, tab), l);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt mc-members.index.gz", e);
        }
        lines = new SoftReference<>(m);
        PARSED.clear();
        return m;
    }

    /** The members of a Minecraft or NeoForge class, or null when 1.21.1 has no such class. */
    public static synchronized Members get(String owner) {
        if (testOverride != null) return testOverride.get(owner);
        Map<String, String> all = lines();
        Members p = PARSED.get(owner);
        if (p != null) return p;
        String line = all.get(owner);
        if (line == null) return null;
        String[] f = line.split("\t", -1);
        p = new Members(split(f.length > 1 ? f[1] : ""), split(f.length > 2 ? f[2] : ""));
        PARSED.put(owner, p);
        return p;
    }

    public static boolean hasClass(String owner) {
        return get(owner) != null;
    }

    private static Set<String> split(String s) {
        Set<String> out = new HashSet<>();
        if (!s.isEmpty()) for (String x : s.split(" ")) out.add(x);
        return out;
    }

    private static SoftReference<Map<String, Set<String>>> statics = new SoftReference<>(null);

    /**
     * True/false when {@code owner} declares the method {@code name+desc} static / not static; null when the class or method is not in the index (or the
     * index is replaced for a test). The static methods come from {@code mc-static.index.gz}, made by the same tool.
     */
    public static synchronized Boolean isStatic(String owner, String nameAndDesc) {
        if (testOverride != null) return null;
        Members mm = get(owner);
        if (mm == null || !mm.methods().contains(nameAndDesc)) return null;
        Map<String, Set<String>> m = statics.get();
        if (m == null) {
            m = new HashMap<>();
            try (InputStream raw = McMembers1201.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/mc-static.index.gz")) {
                if (raw == null) return null;
                BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8));
                for (String l = r.readLine(); l != null; l = r.readLine()) {
                    int tab = l.indexOf('\t');
                    if (tab > 0) m.put(l.substring(0, tab), split(l.substring(tab + 1)));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Corrupt mc-static.index.gz", e);
            }
            statics = new SoftReference<>(m);
        }
        Set<String> s = m.get(owner);
        return s != null && s.contains(nameAndDesc);
    }

    /** Test hook: replaces the index (class to members); null restores the real one. */
    static synchronized void setForTests(Map<String, Members> classes) {
        testOverride = classes;
    }
}
