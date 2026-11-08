package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Names of Minecraft Beta 1.7.3. Mods of that era were compiled against the obfuscated game ({@code uu} is
 * {@code Block} on the client, {@code na} on the server: the two jars were obfuscated separately) and have no
 * SRG ids, so members can only be named through their owner and descriptor. The table chains Babric's intermediary
 * (official client/server to {@code class_17} / {@code method_123}) with biny (intermediary to readable names); both
 * are CC0 and fetched at build time (see {@code :loader:genBetaMappings}), never committed.
 *
 * <p>Table rows ({@link #RESOURCE}, tab separated; side {@code c} = client, {@code s} = server):</p>
 * <pre>
 * C side official named
 * F side officialOwner officialName officialDesc named
 * M side officialOwner officialName officialDesc named
 * </pre>
 */
public final class BetaMappings {
    public static final String RESOURCE = "legacyconnector/mappings/b1.7.3/official-named.tsv";

    public enum Side {
        CLIENT('c'), SERVER('s');

        final char code;

        Side(char code) {
            this.code = code;
        }

        static Side of(char c) {
            return c == 'c' ? CLIENT : SERVER;
        }
    }

    /** One side's names. */
    public static final class Names {
        public final Side side;
        /** official -> named */
        public final Map<String, String> classes = new HashMap<>(1024);
        /** named -> official */
        public final Map<String, String> officialByNamed = new HashMap<>(1024);
        /** {@code owner.name desc} (official) -> named */
        final Map<String, String> methods = new HashMap<>(8192);
        /** {@code owner.name:desc} (official) -> named */
        final Map<String, String> fields = new HashMap<>(8192);
        /** {@code namedOwner.namedName desc(named)} -> official name, for reflection on vanilla members at runtime */
        final Map<String, String> officialMethodByNamed = new HashMap<>(8192);
        /** official names of the members each official class declares, for hierarchy walks */
        final Map<String, java.util.Set<String>> declared = new HashMap<>(1024);

        Names(Side side) {
            this.side = side;
        }

        public boolean isVanilla(String official) {
            return classes.containsKey(official);
        }

        /** Named method declared exactly in {@code owner} (official), or null. */
        public String method(String owner, String name, String desc) {
            return methods.get(owner + "." + name + desc);
        }

        /** Named field declared exactly in {@code owner} (official), or null. */
        public String field(String owner, String name, String desc) {
            return fields.get(owner + "." + name + ":" + desc);
        }

        /** True if the official class declares a method {@code name+desc} (any access). */
        public boolean declaresMethod(String owner, String name, String desc) {
            var set = declared.get(owner);
            return set != null && set.contains(name + desc);
        }

        public int size() {
            return classes.size() + methods.size() + fields.size();
        }

        private final Map<String, List<String>> supertypes = new java.util.concurrent.ConcurrentHashMap<>();
        private final Map<String, String> resolved = new java.util.concurrent.ConcurrentHashMap<>();
        private static final String NONE = "\0";

        /** Superclass then interfaces of a game class, official names of this side (from the shipped b1.7.3 hierarchy). */
        public List<String> supertypes(String official) {
            return supertypes.computeIfAbsent(official, o -> {
                String named = classes.get(o);
                var entry = named == null ? null
                        : org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData.forVersion(org.kylepitcock.legacyconnector.loader.LegacyVersion.VB1_7_3).get(named);
                if (entry == null) return List.of();
                List<String> out = new ArrayList<>();
                if (entry.superName() != null) out.add(officialByNamed.getOrDefault(entry.superName(), entry.superName()));
                for (String i : entry.interfaces()) out.add(officialByNamed.getOrDefault(i, i));
                return List.copyOf(out);
            });
        }

        /** Named method a reference {@code owner.name desc} (official game class) reaches, declared there or inherited; null if none. */
        public String resolveMethod(String owner, String name, String desc) {
            return resolve(owner, name, desc, false);
        }

        /** Named field a reference {@code owner.name:desc} (official game class) reaches; null if none. */
        public String resolveField(String owner, String name, String desc) {
            return resolve(owner, name, desc, true);
        }

        private String resolve(String owner, String name, String desc, boolean field) {
            String key = (field ? "F" : "M") + owner + "." + name + desc;
            String hit = resolved.get(key);
            if (hit == null) {
                String found = walk(owner, name, desc, field, 0);
                hit = found == null ? NONE : found;
                resolved.put(key, hit);
            }
            return NONE.equals(hit) ? null : hit;
        }

        private String walk(String owner, String name, String desc, boolean field, int depth) {
            if (owner == null || depth > 32 || !classes.containsKey(owner)) return null;
            String hit = field ? field(owner, name, desc) : method(owner, name, desc);
            if (hit != null) return hit;
            for (String s : supertypes(owner)) {
                hit = walk(s, name, desc, field, depth + 1);
                if (hit != null) return hit;
            }
            return null;
        }
    }

    private static volatile BetaMappings instance;

    private final Names client = new Names(Side.CLIENT);
    private final Names server = new Names(Side.SERVER);

    private BetaMappings() {}

    public Names side(Side side) {
        return side == Side.CLIENT ? client : server;
    }

    public boolean isEmpty() {
        return client.classes.isEmpty() && server.classes.isEmpty();
    }

    /** The table from the classpath (empty when the jar was built without it). */
    public static BetaMappings get() {
        BetaMappings m = instance;
        if (m != null) return m;
        synchronized (BetaMappings.class) {
            if (instance != null) return instance;
            m = new BetaMappings();
            try (InputStream in = BetaMappings.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                if (in != null) m.read(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + RESOURCE, e);
            }
            return instance = m;
        }
    }

    /** Drops the loaded table (perf.releaseLoaderData). */
    public static synchronized void releaseCaches() {
        instance = null;
    }

    private void read(BufferedReader r) throws IOException {
        for (String l = r.readLine(); l != null; l = r.readLine()) {
            if (l.isEmpty() || l.charAt(0) == '#') continue;
            String[] p = l.split("\t", -1);
            Names n = side(Side.of(p[1].charAt(0)));
            switch (p[0]) {
                case "C" -> {
                    n.classes.put(p[2], p[3]);
                    n.officialByNamed.put(p[3], p[2]);
                }
                case "F" -> {
                    n.fields.put(p[2] + "." + p[3] + ":" + p[4], p[5]);
                    n.declared.computeIfAbsent(p[2], k -> new java.util.HashSet<>()).add(p[3] + ":" + p[4]);
                }
                case "M" -> {
                    n.methods.put(p[2] + "." + p[3] + p[4], p[5]);
                    n.declared.computeIfAbsent(p[2], k -> new java.util.HashSet<>()).add(p[3] + p[4]);
                }
                default -> { }
            }
        }
        for (Names n : List.of(client, server)) {
            for (var e : n.methods.entrySet()) {
                String key = e.getKey();
                int dot = key.indexOf('.');
                int paren = key.indexOf('(', dot);
                String owner = n.classes.get(key.substring(0, dot));
                if (owner == null) continue;
                n.officialMethodByNamed.put(owner + "." + e.getValue() + mapDesc(n, key.substring(paren)), key.substring(dot + 1, paren));
            }
        }
    }

    /** Official descriptor of one side to named. */
    public static String mapDesc(Names n, String desc) {
        StringBuilder sb = new StringBuilder(desc.length() + 16);
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            sb.append(c);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String cls = desc.substring(i + 1, end);
                sb.append(n.classes.getOrDefault(cls, cls)).append(';');
                i = end;
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------- build-time generator

    /** {@code BetaMappings <intermediary.tiny> <biny.tiny> <out.tsv>}: writes {@link #RESOURCE}. */
    public static void main(String[] args) throws IOException {
        Tiny inter = Tiny.read(Path.of(args[0]));
        Tiny biny = Tiny.read(Path.of(args[1]));
        int iInter = inter.ns.indexOf("intermediary");
        int iClient = inter.ns.indexOf("client");
        int iServer = inter.ns.indexOf("server");
        int bInter = biny.ns.indexOf("intermediary");
        int bNamed = biny.ns.indexOf("named");
        if (iInter < 0 || iClient < 0 || iServer < 0 || bInter < 0 || bNamed < 0) {
            throw new IllegalArgumentException("unexpected namespaces " + inter.ns + " / " + biny.ns);
        }
        // intermediary -> named, for classes and members (members keyed by intermediary owner + name + intermediary desc)
        Map<String, String> namedClass = new HashMap<>();
        Map<String, String> namedMember = new HashMap<>();
        for (Tiny.Cls c : biny.classes) {
            String owner = c.names[bInter];
            if (!c.names[bNamed].isEmpty()) namedClass.put(owner, c.names[bNamed]);
            for (Tiny.Member m : c.members) {
                if (!m.names[bNamed].isEmpty()) namedMember.put(m.kind + " " + owner + "." + m.names[bInter] + " " + m.desc, m.names[bNamed]);
            }
        }
        TreeSet<String> rows = new TreeSet<>();
        java.util.Set<String> gameClasses = new java.util.HashSet<>();
        for (Tiny.Cls c : inter.classes) gameClasses.add(c.names[iInter]);
        for (int sideCol : new int[] {iClient, iServer}) {
            char side = sideCol == iClient ? 'c' : 's';
            // A class or member one side lacks carries its intermediary name in that side's column (class_32 is client-only: its server name
            // reads "net/minecraft/class_32").
            Map<String, String> interToOfficial = new HashMap<>();
            for (Tiny.Cls c : inter.classes) if (present(c.names[sideCol], c.names[iInter])) interToOfficial.put(c.names[iInter], c.names[sideCol]);
            for (Tiny.Cls c : inter.classes) {
                String official = c.names[sideCol];
                String owner = c.names[iInter];
                if (!present(official, owner)) continue;
                rows.add("C\t" + side + "\t" + official + "\t" + namedClass.getOrDefault(owner, owner));
                for (Tiny.Member m : c.members) {
                    String name = m.names[sideCol];
                    if (!present(name, m.names[iInter])) continue;
                    String desc = remapDesc(m.desc, interToOfficial, gameClasses);
                    if (desc == null) continue; // refers to a class this side does not have
                    String named = namedMember.getOrDefault(m.kind + " " + owner + "." + m.names[iInter] + " " + m.desc, m.names[iInter]);
                    rows.add((m.kind == 'f' ? "F" : "M") + "\t" + side + "\t" + official + "\t" + name + "\t" + desc + "\t" + named);
                }
            }
        }
        Path out = Path.of(args[2]);
        Files.createDirectories(out.getParent());
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("# Minecraft b1.7.3 official -> named: Babric intermediary (CC0) + biny (CC0). Generated, do not edit.\n");
            for (String row : rows) w.write(row + "\n");
        }
        System.out.println("b1.7.3 mappings: " + rows.size() + " rows -> " + out);
    }

    private static boolean present(String sideName, String intermediary) {
        if (sideName.isEmpty()) return false;
        if (!sideName.equals(intermediary)) return true;
        // unobfuscated names (net/minecraft/client/Minecraft, argo/..., main(), ...) are the same in every column
        String simple = sideName.substring(sideName.lastIndexOf('/') + 1);
        return !(simple.startsWith("class_") || simple.startsWith("method_") || simple.startsWith("field_"));
    }

    /** Intermediary descriptor to one side's official names; null if it names a game class that side does not have. */
    private static String remapDesc(String desc, Map<String, String> classes, java.util.Set<String> gameClasses) {
        StringBuilder sb = new StringBuilder(desc.length());
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            sb.append(c);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String cls = desc.substring(i + 1, end);
                String mapped = classes.get(cls);
                if (mapped == null) {
                    if (gameClasses.contains(cls)) return null;
                    mapped = cls;
                }
                sb.append(mapped).append(';');
                i = end;
            }
        }
        return sb.toString();
    }

    /** Minimal Tiny v2 reader (classes, fields, methods; parameters and comments are skipped). */
    static final class Tiny {
        record Member(char kind, String desc, String[] names) {}

        record Cls(String[] names, List<Member> members) {}

        final List<String> ns;
        final List<Cls> classes = new ArrayList<>();

        private Tiny(List<String> ns) {
            this.ns = ns;
        }

        static Tiny read(Path path) throws IOException {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String[] header = lines.get(0).split("\t", -1);
            if (!header[0].equals("tiny") || !header[1].equals("2")) throw new IOException("not tiny v2: " + path);
            Tiny t = new Tiny(List.of(header).subList(3, header.length));
            int n = t.ns.size();
            Cls current = null;
            for (int i = 1; i < lines.size(); i++) {
                String l = lines.get(i);
                if (l.startsWith("c\t")) {
                    String[] p = l.split("\t", -1);
                    current = new Cls(pad(p, 1, n), new ArrayList<>());
                    t.classes.add(current);
                } else if (current != null && (l.startsWith("\tf\t") || l.startsWith("\tm\t"))) {
                    String[] p = l.substring(1).split("\t", -1);
                    current.members.add(new Member(p[0].charAt(0), p[1], pad(p, 2, n)));
                }
            }
            return t;
        }

        private static String[] pad(String[] p, int from, int n) {
            String[] out = new String[n];
            for (int k = 0; k < n; k++) out[k] = from + k < p.length ? p[from + k] : "";
            return out;
        }
    }
}
