package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipFile;

/**
 * The Minecraft 1.2.5 to 1.4.7 notch table. 1.2.5 mods name the game by the obfuscation of 1.2.5 (client and server obfuscated separately), the
 * 1.4.7 layer by the one of 1.4.7. Ornithe's Calamus intermediary (official to intermediary) and Feather (intermediary to readable names), both CC0, name every
 * class and member of both versions; Feather keeps one readable name for what did not change, so joining on the readable name (class, member name and
 * descriptor in readable names) gives 1.2.5 official to 1.4.7 official. Run by {@code :loader:genMappings125} (opt-in, never part of the default build,
 * output never committed). Rows ({@code legacyconnector/mappings/1.2.5/notch-to-147.tsv}, tab separated, side {@code c} client / {@code s} server):
 * <pre>
 * C side official125 official147
 * F side owner125 name125 desc125 owner147 name147 desc147
 * M side owner125 name125 desc125 owner147 name147 desc147
 * </pre>
 * The report ({@code join-report.txt}) lists what has no 1.4.7 counterpart.
 */
public final class Mc125Mappings {
    /** One version's names: official to readable, with readable members keyed {@code owner.name desc} (all readable). */
    static final class Names {
        final Map<String, String> classes = new HashMap<>();
        /** readable key to {officialOwner, officialName, officialDesc} */
        final Map<String, String[]> fields = new HashMap<>();
        final Map<String, String[]> methods = new HashMap<>();
        /** official class, official member name, official desc: readable ones, in file order, for iteration */
        final List<String[]> members = new ArrayList<>();
        final Map<String, String> officialDescToReadable = new HashMap<>();

        String readableDesc(String desc) {
            return officialDescToReadable.computeIfAbsent(desc, d -> mapDesc(d, classes));
        }
    }

    private record Row(String kind, String owner, String desc, String from, String to) {}

    private Mc125Mappings() {}

    /** Where the table goes below the output folder: the resource path the loader reads it from. */
    static Path tableFile(Path out) {
        return out.resolve("legacyconnector/mappings/1.2.5/notch-to-147.tsv");
    }

    /** Parses a tiny v2 file (classes, fields and methods only) into rows grouped by class. */
    private static Map<String, List<Row>> parseTiny(Path jar) throws IOException {
        Map<String, List<Row>> out = new java.util.LinkedHashMap<>();
        try (ZipFile zf = new ZipFile(jar.toFile()); InputStream in = zf.getInputStream(zf.getEntry("mappings/mappings.tiny"))) {
            String[] lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n");
            String cur = null;
            for (int i = 1; i < lines.length; i++) {
                String[] p = lines[i].replace("\r", "").split("\t", -1);
                if (p.length >= 3 && p[0].equals("c")) {
                    cur = p[1];
                    out.computeIfAbsent(cur, k -> new ArrayList<>()).add(new Row("c", cur, null, p[1], p[2]));
                } else if (cur != null && p.length >= 5 && p[0].isEmpty() && (p[1].equals("m") || p[1].equals("f"))) {
                    out.get(cur).add(new Row(p[1], cur, p[2], p[3], p[4]));
                }
            }
        }
        return out;
    }

    /** Maps the class names inside a descriptor. */
    static String mapDesc(String desc, Map<String, String> classes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String n = desc.substring(i + 1, end);
                sb.append('L').append(classes.getOrDefault(n, n)).append(';');
                i = end;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** official to intermediary (calamus) chained with intermediary to readable (feather) into one version's names. */
    static Names load(Path calamus, Path feather) throws IOException {
        Map<String, List<Row>> cal = parseTiny(calamus);
        Map<String, List<Row>> fea = parseTiny(feather);
        Map<String, String> interToReadable = new HashMap<>();
        Map<String, String> interMembers = new HashMap<>(); // interOwner.interName interDesc(inter) to readable name
        for (var e : fea.entrySet()) {
            for (Row r : e.getValue()) {
                if (r.kind.equals("c")) interToReadable.put(r.from, r.to);
                else interMembers.put(r.kind + " " + e.getKey() + "." + r.from + " " + r.desc, r.to);
            }
        }
        Names n = new Names();
        Map<String, String> officialToInter = new HashMap<>();
        for (var e : cal.entrySet()) {
            Row c = e.getValue().get(0);
            officialToInter.put(c.from, c.to);
            n.classes.put(c.from, interToReadable.getOrDefault(c.to, c.to));
        }
        for (var e : cal.entrySet()) {
            String ownerInter = officialToInter.get(e.getKey());
            for (Row r : e.getValue()) {
                if (r.kind.equals("c")) continue;
                String interDesc = mapDesc(r.desc, officialToInter);
                String readable = interMembers.getOrDefault(r.kind + " " + ownerInter + "." + r.to + " " + interDesc, r.to);
                String key = n.classes.get(e.getKey()) + "." + readable + " " + n.readableDesc(r.desc);
                (r.kind.equals("m") ? n.methods : n.fields).putIfAbsent(key, new String[] {e.getKey(), r.from, r.desc});
                n.members.add(new String[] {r.kind, e.getKey(), r.from, r.desc, key});
            }
        }
        return n;
    }

    /** Area of a readable class name for the report: its package below net/minecraft, two levels deep. */
    static String area(String readable) {
        String s = readable.startsWith("net/minecraft/") ? readable.substring("net/minecraft/".length()) : readable;
        String[] parts = s.split("/");
        if (parts.length <= 1) return "(root)";
        return parts.length == 2 ? parts[0] : parts[0] + "/" + parts[1];
    }

    /**
     * args: calamus1.2.5-client feather1.2.5-client calamus1.2.5-server feather1.2.5-server calamus1.4.7 feather1.4.7 outDir
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 7) throw new IllegalArgumentException("usage: <calamus125c> <feather125c> <calamus125s> <feather125s> <calamus147> <feather147> <outDir>");
        Names v147 = load(Path.of(args[4]), Path.of(args[5]));
        Map<String, String> readableTo147 = new HashMap<>();
        for (var e : v147.classes.entrySet()) readableTo147.put(e.getValue(), e.getKey());
        Path out = Path.of(args[6]);
        Files.createDirectories(tableFile(out).getParent());
        StringBuilder report = new StringBuilder("1.2.5 to 1.4.7 join (class, member name and descriptor in Feather names)\n");
        try (BufferedWriter table = Files.newBufferedWriter(tableFile(out), StandardCharsets.UTF_8)) {
            String[] sides = {"c", "s"};
            for (int s = 0; s < 2; s++) {
                Names v125 = load(Path.of(args[s * 2]), Path.of(args[s * 2 + 1]));
                int classesJoined = 0, membersJoined = 0, membersTotal = 0;
                Map<String, int[]> areaClasses = new TreeMap<>(); // area -> {classes, joined, members, joined members}
                Map<String, List<String>> lostClasses = new TreeMap<>();
                for (var e : v125.classes.entrySet()) {
                    String readable = e.getValue();
                    String off147 = readableTo147.get(readable);
                    int[] a = areaClasses.computeIfAbsent(area(readable), k -> new int[4]);
                    a[0]++;
                    if (off147 == null) {
                        lostClasses.computeIfAbsent(area(readable), k -> new ArrayList<>()).add(readable);
                        continue;
                    }
                    a[1]++;
                    classesJoined++;
                    table.write("C\t" + sides[s] + "\t" + e.getKey() + "\t" + off147 + "\n");
                }
                for (String[] m : v125.members) {
                    membersTotal++;
                    String ownerReadable = v125.classes.get(m[1]);
                    int[] a = areaClasses.computeIfAbsent(area(ownerReadable), k -> new int[4]);
                    a[2]++;
                    boolean method = m[0].equals("m");
                    String[] to = (method ? v147.methods : v147.fields).get(m[4]);
                    if (to == null) continue;
                    a[3]++;
                    membersJoined++;
                    table.write((method ? "M" : "F") + "\t" + sides[s] + "\t" + m[1] + "\t" + m[2] + "\t" + m[3] + "\t" + to[0] + "\t" + to[1] + "\t" + to[2] + "\n");
                }
                report.append("\n== ").append(s == 0 ? "client" : "server").append(": classes joined ").append(classesJoined).append(" of ").append(v125.classes.size())
                        .append(", members joined ").append(membersJoined).append(" of ").append(membersTotal).append("\n");
                report.append("area: classes (joined) members (joined)\n");
                for (var e : areaClasses.entrySet()) {
                    int[] a = e.getValue();
                    report.append(String.format("  %-34s %4d (%4d) %6d (%6d)%n", e.getKey(), a[0], a[1], a[2], a[3]));
                }
                report.append("classes with no 1.4.7 counterpart, by area:\n");
                for (var e : lostClasses.entrySet()) {
                    report.append("  ").append(e.getKey()).append(": ");
                    List<String> names = e.getValue();
                    java.util.Collections.sort(names);
                    for (int i = 0; i < names.size(); i++) report.append(i > 0 ? ", " : "").append(names.get(i).substring(names.get(i).lastIndexOf('/') + 1));
                    report.append("\n");
                }
            }
        }
        Files.writeString(out.resolve("join-report.txt"), report.toString(), StandardCharsets.UTF_8);
        System.out.println("1.2.5 to 1.4.7 join written to " + out.toAbsolutePath());
        System.out.print(report.substring(0, Math.min(report.length(), 600)));
    }
}
