package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SRG id -> Mojang member name for Minecraft 1.20.1. Forge 1.20.1 mods are SRG-named at run time ({@code m_46859_}, {@code f_46441_}) while
 * NeoForge 1.21.1 runs with Mojang names; SRG ids are unique, so the owner is not needed (as in {@link McpMappings}).
 *
 * <p>The table is built at build time by chaining MCPConfig's {@code config/joined.tsrg} (obf, srg, id) with the version's Mojang
 * {@code client.txt} (Mojang name -> obf) on the obfuscated class, member name and descriptor. Mojang's mapping file may not be
 * redistributed complete and unmodified, so the table is generated into {@code build/} and shipped only inside the built jar, never committed.
 */
public final class SrgMojangTable {
    public static final String RESOURCE = "legacyconnector/mappings/1.20.1/srg-mojang.tsv";
    private static final Pattern CLASS = Pattern.compile("^(\\S+) -> (\\S+):$");
    private static final Pattern METHOD = Pattern.compile("^\\s+(?:\\d+:\\d+:)?(\\S+) (\\S+)\\((.*?)\\)(?::\\d+:\\d+)? -> (\\S+)$");
    private static final Pattern FIELD = Pattern.compile("^\\s+(\\S+) (\\S+) -> (\\S+)$");
    private static final Pattern SRG_ID = Pattern.compile("^[mf]_\\d+_$");

    private static volatile Map<String, String> table;

    private SrgMojangTable() {}

    /** The table from the classpath (empty when the jar was built without it). */
    public static synchronized Map<String, String> load() {
        if (table != null) return table;
        Map<String, String> map = new HashMap<>(80_000);
        try (InputStream in = SrgMojangTable.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String l = r.readLine(); l != null; l = r.readLine()) {
                    int t = l.indexOf('\t');
                    if (t > 0) map.put(l.substring(0, t), l.substring(t + 1));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RESOURCE, e);
        }
        return table = map;
    }

    public static synchronized void releaseCaches() {
        table = null;
    }

    /** Build step: {@code <joined.tsrg> <client.txt> <out.tsv>}. */
    public static void main(String[] args) throws IOException {
        Map<String, String> out = generate(Path.of(args[0]), Path.of(args[1]));
        Files.createDirectories(Path.of(args[2]).toAbsolutePath().getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of(args[2])))) {
            out.forEach((k, v) -> w.println(k + "\t" + v));
        }
        System.out.println("SRG -> Mojang 1.20.1: " + out.size() + " members");
    }

    public static Map<String, String> generate(Path tsrg, Path mojang) throws IOException {
        Map<String, String> classes = new HashMap<>(); // mojang (dots) -> obf (slashes)
        record Raw(String cls, boolean method, String name, String ret, String args, String obf) {}
        java.util.List<Raw> raw = new java.util.ArrayList<>();
        String cur = null;
        try (BufferedReader r = Files.newBufferedReader(mojang, StandardCharsets.UTF_8)) {
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                if (l.startsWith("#") || l.isBlank()) continue;
                Matcher m = CLASS.matcher(l);
                if (m.matches()) {
                    cur = m.group(1);
                    classes.put(cur, m.group(2).replace('.', '/'));
                    continue;
                }
                m = METHOD.matcher(l);
                if (m.matches()) {
                    raw.add(new Raw(cur, true, m.group(2), m.group(1), m.group(3), m.group(4)));
                    continue;
                }
                m = FIELD.matcher(l);
                if (m.matches()) raw.add(new Raw(cur, false, m.group(2), m.group(1), null, m.group(3)));
            }
        }
        Map<String, String> members = new HashMap<>(); // obfClass|obfName|obfDesc-or-"" -> mojang name
        for (Raw x : raw) {
            String oc = classes.get(x.cls());
            if (x.method()) {
                StringBuilder d = new StringBuilder("(");
                if (!x.args().isEmpty()) for (String a : x.args().split(",")) d.append(desc(a, classes));
                d.append(')').append(desc(x.ret(), classes));
                members.put(oc + "|" + x.obf() + "|" + d, x.name());
            } else {
                members.put(oc + "|" + x.obf() + "|", x.name());
            }
        }
        Map<String, String> out = new TreeMap<>((a, b) -> {
            int c = Character.compare(a.charAt(0), b.charAt(0));
            return c != 0 ? c : Long.compare(Long.parseLong(a.substring(2, a.length() - 1)), Long.parseLong(b.substring(2, b.length() - 1)));
        });
        String obfClass = null;
        try (BufferedReader r = Files.newBufferedReader(tsrg, StandardCharsets.UTF_8)) {
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                if (l.startsWith("tsrg2")) continue;
                if (!l.startsWith("\t")) {
                    obfClass = l.split(" ")[0];
                    continue;
                }
                if (l.startsWith("\t\t")) continue;
                String[] p = l.trim().split(" ");
                String srg, name;
                if (p.length == 3) {
                    srg = p[1];
                    name = members.get(obfClass + "|" + p[0] + "|");
                } else if (p.length == 4) {
                    srg = p[2];
                    name = members.get(obfClass + "|" + p[0] + "|" + p[1]);
                } else continue;
                if (name != null && SRG_ID.matcher(srg).matches()) out.put(srg, name);
            }
        }
        return out;
    }

    private static String desc(String t, Map<String, String> classes) {
        int dims = 0;
        while (t.endsWith("[]")) {
            dims++;
            t = t.substring(0, t.length() - 2);
        }
        String base = switch (t) {
            case "int" -> "I";
            case "long" -> "J";
            case "double" -> "D";
            case "float" -> "F";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "void" -> "V";
            default -> "L" + classes.getOrDefault(t, t.replace('.', '/')) + ";";
        };
        return "[".repeat(dims) + base;
    }
}
