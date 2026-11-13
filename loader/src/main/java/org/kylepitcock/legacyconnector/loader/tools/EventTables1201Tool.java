package org.kylepitcock.legacyconnector.loader.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * Generates the event tables of the 1.20.1 pipeline from the real jars (data only; nothing of the jars is shipped):
 * <ul>
 * <li>{@code neoforge-abstract-events.tsv}: every abstract NeoForge event (a class below {@code net/neoforged/bus/api/Event}) and the concrete events
 * below it, read by {@link org.kylepitcock.legacyconnector.loader.transform.Subscribers1201};</li>
 * <li>a report of Forge 1.20.1 event classes that no rename table maps, with the NeoForge class of the same simple name when there is exactly one
 * (candidate rows), rename rows whose target does not exist, and rename rows whose target is abstract.</li>
 * </ul>
 *
 * <p>The idea of generating the abstract-event list from the NeoForge jars is Retromod's ({@code scripts/NeoForgeEventTable.java}); the code is our own.
 * Portions adapted from Retromod (https://github.com/Bownlux/Retromod), MIT License, Copyright (c) 2026 Bownlux.
 *
 * <p>usage: {@code EventTables1201Tool --neoforge a.jar[,b.jar...] [--forge forge-universal.jar] [--renames dir] [--out abstract.tsv] [--baseline old.tsv] [--facades src/main/java/legacyconnector/v1_20_1]}.
 * The NeoForge jars are the merged game jar of {@code build/moddev/artifacts}, the bus jar and the FML loader jar.
 */
public final class EventTables1201Tool {
    private EventTables1201Tool() {}

    private static final String NEO_EVENT = "net/neoforged/bus/api/Event";
    private static final String FORGE_EVENT = "net/minecraftforge/eventbus/api/Event";
    private static final String MOD_BUS = "net/neoforged/fml/event/IModBusEvent";

    /** Name, super, interfaces and access of one class. */
    record Shape(String name, String sup, String[] ifaces, int access) {}

    static Map<String, Shape> read(List<Path> jars, String prefix) throws IOException {
        Map<String, Shape> out = new HashMap<>();
        for (Path jar : jars) {
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                for (var en = zip.entries(); en.hasMoreElements(); ) {
                    var e = en.nextElement();
                    String n = e.getName();
                    if (!n.endsWith(".class") || n.startsWith("META-INF/") || prefix != null && !n.startsWith(prefix)) continue;
                    ClassNode c = new ClassNode();
                    new ClassReader(zip.getInputStream(e).readAllBytes()).accept(c, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    out.put(c.name, new Shape(c.name, c.superName, c.interfaces.toArray(new String[0]), c.access));
                }
            }
        }
        return out;
    }

    static boolean isEvent(Map<String, Shape> classes, String name, String root) {
        for (String c = name; c != null; ) {
            if (c.equals(root)) return true;
            Shape s = classes.get(c);
            c = s == null ? null : s.sup();
        }
        return false;
    }

    static boolean modBus(Map<String, Shape> classes, String name) {
        if (name == null) return false;
        if (name.equals(MOD_BUS)) return true;
        Shape s = classes.get(name);
        if (s == null) return false;
        for (String i : s.ifaces()) if (modBus(classes, i)) return true;
        return modBus(classes, s.sup());
    }

    static boolean isAbstract(Map<String, Shape> classes, String name) {
        Shape s = classes.get(name);
        return s != null && !name.equals(NEO_EVENT) && (s.access() & Opcodes.ACC_ABSTRACT) != 0 && (s.access() & Opcodes.ACC_INTERFACE) == 0;
    }

    /** Abstract event to the concrete events whose superclass chain contains it (every descendant, not only the topmost ones). */
    static TreeMap<String, TreeSet<String>> abstractEvents(Map<String, Shape> classes) {
        TreeMap<String, TreeSet<String>> rows = new TreeMap<>();
        for (Shape s : classes.values()) {
            if ((s.access() & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0 || !isEvent(classes, s.name(), NEO_EVENT)) continue;
            for (String p = s.sup(); p != null && isAbstract(classes, p); p = classes.get(p).sup()) {
                if (modBus(classes, p) == modBus(classes, s.name())) rows.computeIfAbsent(p, k -> new TreeSet<>()).add(s.name());
            }
        }
        // abstract events without a concrete descendant on their bus still are abstract: keep them with an empty child list
        for (Shape s : classes.values()) {
            if (isAbstract(classes, s.name()) && isEvent(classes, s.name(), NEO_EVENT)) rows.computeIfAbsent(s.name(), k -> new TreeSet<>());
        }
        return rows;
    }

    static Map<String, String> readRenames(Path dir) throws IOException {
        Map<String, String> map = new TreeMap<>();
        for (String f : List.of("forge-event-renames.tsv", "forge-fml-renames.tsv", "forge-curated-renames.tsv")) {
            Path p = dir.resolve(f);
            if (!Files.exists(p)) continue;
            for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (l.startsWith("#") || l.isBlank()) continue;
                String[] a = l.strip().split("\t");
                if (a.length == 2) map.put(a[0], a[1]);
            }
        }
        return map;
    }

    static String simple(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    public static void main(String[] args) throws Exception {
        List<Path> neo = new ArrayList<>();
        Path forge = null;
        Path renames = Path.of("loader/src/main/resources/legacyconnector/forward/1.20.1");
        Path out = null;
        Path baseline = null;
        Path facades = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--neoforge" -> { for (String p : args[++i].split(",")) neo.add(Path.of(p)); }
                case "--forge" -> forge = Path.of(args[++i]);
                case "--renames" -> renames = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--facades" -> facades = Path.of(args[++i]);
                case "--baseline" -> baseline = Path.of(args[++i]);
                default -> throw new IllegalArgumentException(args[i]);
            }
        }
        Map<String, Shape> neoClasses = read(neo, null);
        TreeMap<String, TreeSet<String>> abs = abstractEvents(neoClasses);
        StringBuilder tsv = new StringBuilder("# NeoForge 1.21.1 abstract events (NeoForge refuses a listener on one) and the concrete events below them on the same bus; "
                + "written by loader/tools/EventTables1201Tool from the NeoForge merged jar, the bus jar and the FML loader jar. Columns: abstract event, bus (GAME or MOD), concrete events.\n");
        abs.forEach((p, ch) -> tsv.append(p).append('\t').append(modBus(neoClasses, p) ? "MOD" : "GAME").append('\t').append(String.join(",", ch)).append('\n'));
        if (out != null) Files.writeString(out, tsv.toString(), StandardCharsets.UTF_8);
        System.out.println("abstract NeoForge events: " + abs.size());

        if (baseline != null) {
            List<String> old = new ArrayList<>();
            for (String l : Files.readAllLines(baseline)) if (!l.startsWith("#") && !l.isBlank()) old.add(l.split("\t")[0]);
            for (String p : abs.keySet()) if (!old.contains(p)) System.out.println("abstract, not in baseline: " + p);
            for (String p : old) if (!abs.containsKey(p)) System.out.println("in baseline, not abstract (or absent): " + p);
        }

        Map<String, String> table = readRenames(renames);
        java.util.Set<String> facadeNames = new java.util.HashSet<>();
        if (facades != null) {
            Path root = facades;
            try (var walk = Files.walk(root)) {
                walk.filter(f -> f.toString().endsWith(".java")).forEach(f -> facadeNames.add(root.relativize(f).toString().replace('\\', '/').replaceAll("\\.java$", "")));
            }
        }
        for (var e : table.entrySet()) {
            if (!e.getKey().contains("Event")) continue;
            if (!neoClasses.containsKey(e.getValue())) System.out.println("ROW-TARGET-MISSING\t" + e.getKey() + "\t" + e.getValue());
            else if (isAbstract(neoClasses, e.getValue())) System.out.println("ROW-TARGET-ABSTRACT\t" + e.getKey() + "\t" + e.getValue());
        }

        if (forge != null) {
            Map<String, Shape> forgeClasses = read(List.of(forge), "net/minecraftforge/");
            Map<String, List<String>> bySimple = new HashMap<>();
            for (Shape s : neoClasses.values()) {
                if (s.name().startsWith("net/neoforged/") && isEvent(neoClasses, s.name(), NEO_EVENT)) bySimple.computeIfAbsent(simple(s.name()), k -> new ArrayList<>()).add(s.name());
            }
            int events = 0, ok = 0, viaFacade = 0;
            TreeSet<String> abstractTargets = new TreeSet<>();
            for (Shape s : new TreeMap<>(forgeClasses).values()) {
                if (!isEvent(forgeClasses, s.name(), FORGE_EVENT) || s.name().equals(FORGE_EVENT)) continue;
                if ((s.access() & Opcodes.ACC_PUBLIC) == 0 && (s.access() & Opcodes.ACC_PROTECTED) == 0) continue;
                events++;
                if (facadeNames.contains("legacyconnector/v1_20_1/" + s.name()) || facadeNames.contains(s.name())) { viaFacade++; continue; }
                String target = effective(s.name(), table);
                if (neoClasses.containsKey(target)) {
                    ok++;
                    if (isAbstract(neoClasses, target)) abstractTargets.add(target);
                    continue;
                }
                List<String> c = bySimple.getOrDefault(simple(s.name()), List.of());
                if (c.size() == 1) System.out.println("CANDIDATE-ROW\t" + s.name() + "\t" + c.get(0));
                else System.out.println("NO-COUNTERPART\t" + s.name() + "\t" + (c.isEmpty() ? "no NeoForge event of that name" : "ambiguous: " + String.join(",", c)));
            }
            System.out.println("Forge 1.20.1 events: " + events + ", resolved by rows/package moves: " + ok + ", by a facade: " + viaFacade);
            for (String t : abstractTargets) System.out.println("FORGE-EVENT-NOW-ABSTRACT\t" + t);
        }
    }

    /** The name {@code Forge1201Transformer.mapClass} gives a Forge class that has no facade. */
    static String effective(String name, Map<String, String> table) {
        String direct = table.get(name);
        if (direct != null) return direct;
        int dollar = name.indexOf('$');
        if (dollar > 0) {
            String outer = effective(name.substring(0, dollar), table);
            if (!outer.equals(name.substring(0, dollar))) return outer + name.substring(dollar);
        }
        for (String[] m : new String[][] {{"net/minecraftforge/fml/", "net/neoforged/fml/"}, {"net/minecraftforge/eventbus/", "net/neoforged/bus/"},
                {"net/minecraftforge/api/", "net/neoforged/api/"}, {"net/minecraftforge/forgespi/", "net/neoforged/neoforgespi/"},
                {"net/minecraftforge/", "net/neoforged/neoforge/"}}) {
            if (name.startsWith(m[0])) return m[1] + name.substring(m[0].length());
        }
        return name;
    }
}
