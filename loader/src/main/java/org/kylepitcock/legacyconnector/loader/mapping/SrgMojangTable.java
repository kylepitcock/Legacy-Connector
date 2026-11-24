package org.kylepitcock.legacyconnector.loader.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SRG id -> Mojang member name for Minecraft 1.20.1. Forge 1.20.1 mods are SRG-named at run time
 * ({@code m_46859_}, {@code f_46441_}) while NeoForge 1.21.1 runs with Mojang names.
 *
 * <p>The table is built at build time by joining MCPConfig's {@code config/joined.tsrg} with
 * Mojang's 1.20.1 {@code client.txt} using the obfuscated owner, member name, and (for methods)
 * full descriptor. The generated output is deliberately only a global SRG member-id -> readable
 * member-name table. It does not retain or provide class-name or descriptor-type remapping, and it
 * is not a 1.20.1 -> 1.21.1 mapping edge.</p>
 *
 * <p>Mojang's full mapping file is not committed. Only the derived table is generated into
 * {@code build/}; redistribution of source mapping artifacts and licensing of derived data require
 * separate review.</p>
 */
public final class SrgMojangTable {
    public static final String RESOURCE = "legacyconnector/mappings/1.20.1/srg-mojang.tsv";
    private static final Pattern CLASS = Pattern.compile("^(\\S+) -> (\\S+):$");
    private static final Pattern METHOD = Pattern.compile("^\\s+(?:\\d+:\\d+:)?(\\S+) (\\S+)\\((.*?)\\)(?::\\d+:\\d+)? -> (\\S+)$");
    private static final Pattern FIELD = Pattern.compile("^\\s+(\\S+) (\\S+) -> (\\S+)$");
    // func_/field_ ids: EXPERIMENTAL 1.16.5 scoping (opt-in, tsrg1 joined file)
    private static final Pattern SRG_METHOD = Pattern.compile("^(m_\\d+_|func_\\d+_[A-Za-z]+_?)$");
    private static final Pattern SRG_FIELD = Pattern.compile("^(f_\\d+_|field_\\d+_[A-Za-z]+_?)$");

    private static final Pattern SRG_NUMBER = Pattern.compile("[0-9]+");
    private static long srgNumber(String id) {
        Matcher m = SRG_NUMBER.matcher(id);
        return m.find() ? Long.parseLong(m.group()) : 0L;
    }
    private static final Comparator<String> SRG_ORDER = (left, right) -> {
        int kind = Character.compare(left.charAt(0), right.charAt(0));
        if (kind != 0) return kind;
        int number = Long.compare(srgNumber(left), srgNumber(right));
        return number != 0 ? number : left.compareTo(right);
    };

    private static volatile Map<String, String> table;

    private SrgMojangTable() {}

    /**
     * Counts source and joined rows. A method is considered matched only when owner, obfuscated
     * name, and complete JVM descriptor identify the same Mojang mapping row.
     */
    public record Coverage(
            int mojangClassRows,
            int mojangMethodRows,
            int mojangFieldRows,
            int joinedClassRows,
            int joinedMethodRows,
            int joinedFieldRows,
            int matchedMethods,
            int matchedFields,
            int duplicateMemberKeys,
            int duplicateSrgIds) {
        public int unmatchedMethods() {
            return joinedMethodRows - matchedMethods;
        }

        public int unmatchedFields() {
            return joinedFieldRows - matchedFields;
        }

        public int outputRows() {
            return matchedMethods + matchedFields - duplicateSrgIds;
        }
    }

    public record GenerationResult(Map<String, String> table, Coverage coverage, Map<String, String> classRenames) {
        public GenerationResult(Map<String, String> table, Coverage coverage) {
            this(table, coverage, Map.of());
        }
        public GenerationResult {
            TreeMap<String, String> ordered = new TreeMap<>(SRG_ORDER);
            ordered.putAll(table);
            table = Collections.unmodifiableMap(ordered);
        }
    }

    /** The table from the classpath (empty when the jar was built without it). */
    public static synchronized Map<String, String> load() {
        if (table != null) return table;
        Map<String, String> map = new HashMap<>(80_000);
        try (InputStream in = SrgMojangTable.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    for (String line; (line = reader.readLine()) != null;) {
                        int tab = line.indexOf('\t');
                        if (tab <= 0 || tab == line.length() - 1) {
                            throw new IOException("Malformed mapping row in " + RESOURCE);
                        }
                        String srg = line.substring(0, tab);
                        String name = line.substring(tab + 1);
                        if (!(SRG_METHOD.matcher(srg).matches() || SRG_FIELD.matcher(srg).matches())) {
                            throw new IOException("Unsupported SRG member id in " + RESOURCE + ": " + srg);
                        }
                        String previous = map.putIfAbsent(srg, name);
                        if (previous != null && !previous.equals(name)) {
                            throw new IOException("Conflicting mapping rows in " + RESOURCE + " for " + srg);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + RESOURCE, e);
        }
        return table = Collections.unmodifiableMap(map);
    }

    public static synchronized void releaseCaches() {
        table = null;
    }

    /** Build step: {@code <joined.tsrg> <client.txt> <out.tsv> [coverage.properties]}. */
    public static void main(String[] args) throws IOException {
        if (args.length < 3 || args.length > 5) {
            throw new IllegalArgumentException(
                    "Usage: SrgMojangTable <joined.tsrg> <client.txt> <out.tsv> [coverage.properties]");
        }
        GenerationResult result = generateDetailed(Path.of(args[0]), Path.of(args[1]));
        Path output = Path.of(args[2]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            result.table().forEach((key, value) -> writer.println(key + "\t" + value));
            if (writer.checkError()) throw new IOException("Could not write generated mapping table: " + output);
        }
        Coverage coverage = result.coverage();
        if (args.length == 5 && !result.classRenames().isEmpty()) { // EXPERIMENTAL 1.16.5 scoping: class rows (MCP -> Mojang) in the tsv format of the forward tables
            Path classes = Path.of(args[4]);
            Files.createDirectories(classes.toAbsolutePath().getParent());
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(classes, StandardCharsets.UTF_8))) {
                result.classRenames().forEach((from, to) -> writer.println(from + "\t" + to));
            }
        }
        if (args.length >= 4) {
            Path report = Path.of(args[3]);
            Files.createDirectories(report.toAbsolutePath().getParent());
            String reportText = "mojangClassRows=" + coverage.mojangClassRows() + "\n"
                    + "mojangMethodRows=" + coverage.mojangMethodRows() + "\n"
                    + "mojangFieldRows=" + coverage.mojangFieldRows() + "\n"
                    + "joinedClassRows=" + coverage.joinedClassRows() + "\n"
                    + "joinedMethodRows=" + coverage.joinedMethodRows() + "\n"
                    + "joinedFieldRows=" + coverage.joinedFieldRows() + "\n"
                    + "matchedMethods=" + coverage.matchedMethods() + "\n"
                    + "matchedFields=" + coverage.matchedFields() + "\n"
                    + "unmatchedMethods=" + coverage.unmatchedMethods() + "\n"
                    + "unmatchedFields=" + coverage.unmatchedFields() + "\n"
                    + "duplicateMemberKeys=" + coverage.duplicateMemberKeys() + "\n"
                    + "duplicateSrgIds=" + coverage.duplicateSrgIds() + "\n"
                    + "outputRows=" + coverage.outputRows() + "\n";
            Files.writeString(report, reportText, StandardCharsets.UTF_8);
        }
        System.out.printf(
                "SRG -> Mojang 1.20.1: %d member rows (methods %d/%d, fields %d/%d; %d duplicate member keys)%n",
                result.table().size(), coverage.matchedMethods(), coverage.joinedMethodRows(),
                coverage.matchedFields(), coverage.joinedFieldRows(), coverage.duplicateMemberKeys());
        System.out.println("Coverage is member-name-only; class names and descriptor types are not emitted.");
    }

    /** Backwards-compatible map-only entry point. */
    public static Map<String, String> generate(Path tsrg, Path mojang) throws IOException {
        return generateDetailed(tsrg, mojang).table();
    }

    public static GenerationResult generateDetailed(Path tsrg, Path mojang) throws IOException {
        if (tsrg == null || !Files.isRegularFile(tsrg)) {
            throw new IOException("Missing MCPConfig joined.tsrg input: " + tsrg);
        }
        if (mojang == null || !Files.isRegularFile(mojang)) {
            throw new IOException("Missing Mojang client mapping input: " + mojang);
        }

        Map<String, String> classes = new HashMap<>();
        Map<String, String> classNames = new HashMap<>(); // tsrg1 (1.16.5): obfuscated class -> MCP class name
        record Raw(String cls, boolean method, String name, String ret, String args, String obf) {}
        java.util.List<Raw> raw = new java.util.ArrayList<>();
        int mojangClassRows = 0;
        int mojangMethodRows = 0;
        int mojangFieldRows = 0;
        String currentClass = null;
        try (BufferedReader reader = Files.newBufferedReader(mojang, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.startsWith("#") || line.isBlank()) continue;
                Matcher matcher = CLASS.matcher(line);
                if (matcher.matches()) {
                    currentClass = matcher.group(1);
                    String obfuscatedName = matcher.group(2).replace('.', '/');
                    String previous = classes.putIfAbsent(currentClass, obfuscatedName);
                    if (previous != null && !previous.equals(obfuscatedName)) {
                        throw new IOException("Conflicting Mojang class mappings for " + currentClass
                                + ": " + previous + " vs " + obfuscatedName);
                    }
                    mojangClassRows++;
                    continue;
                }
                matcher = METHOD.matcher(line);
                if (matcher.matches()) {
                    if (currentClass == null) throw new IOException("Mojang method mapping appears before a class row: " + line);
                    raw.add(new Raw(currentClass, true, matcher.group(2), matcher.group(1),
                            matcher.group(3), matcher.group(4)));
                    mojangMethodRows++;
                    continue;
                }
                matcher = FIELD.matcher(line);
                if (matcher.matches()) {
                    if (currentClass == null) throw new IOException("Mojang field mapping appears before a class row: " + line);
                    raw.add(new Raw(currentClass, false, matcher.group(2), matcher.group(1), null, matcher.group(3)));
                    mojangFieldRows++;
                }
            }
        }

        Map<String, String> members = new HashMap<>();
        int duplicateMemberKeys = 0;
        for (Raw mapping : raw) {
            String obfuscatedClass = classes.get(mapping.cls());
            if (obfuscatedClass == null) continue;
            String key;
            if (mapping.method()) {
                StringBuilder descriptor = new StringBuilder("(");
                if (!mapping.args().isEmpty()) {
                    for (String argument : mapping.args().split(",")) {
                        descriptor.append(desc(argument, classes));
                    }
                }
                descriptor.append(')').append(desc(mapping.ret(), classes));
                key = obfuscatedClass + "|" + mapping.obf() + "|" + descriptor;
            } else {
                key = obfuscatedClass + "|" + mapping.obf() + "|";
            }
            String previous = members.putIfAbsent(key, mapping.name());
            if (previous != null) {
                if (!previous.equals(mapping.name())) {
                    throw new IOException("Conflicting Mojang member mappings for obfuscated key "
                            + key + ": " + previous + " vs " + mapping.name());
                }
                duplicateMemberKeys++;
            }
        }

        Map<String, String> output = new TreeMap<>(SRG_ORDER);
        int joinedClassRows = 0;
        int joinedMethodRows = 0;
        int joinedFieldRows = 0;
        int matchedMethods = 0;
        int matchedFields = 0;
        int duplicateSrgIds = 0;
        String obfuscatedClass = null;
        String firstLine;
        try (BufferedReader head = Files.newBufferedReader(tsrg, StandardCharsets.UTF_8)) { firstLine = head.readLine(); }
        boolean tsrg1 = firstLine != null && !firstLine.startsWith("tsrg2");
        try (BufferedReader reader = Files.newBufferedReader(tsrg, StandardCharsets.UTF_8)) {
            int lineNumber = 0;
            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("tsrg2")) continue;
                if (!line.startsWith("\t")) {
                    String[] classParts = line.trim().split("\\s+");
                    if (classParts.length == 0 || classParts[0].isBlank()) {
                        throw new IOException("Malformed TSRG2 class row at " + tsrg + ":" + lineNumber);
                    }
                    obfuscatedClass = classParts[0];
                    if (tsrg1 && classParts.length > 1) classNames.put(obfuscatedClass, classParts[1]);
                    joinedClassRows++;
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                String srg;
                String readableName;
                boolean method;
                if (tsrg1 && parts.length == 2 && SRG_FIELD.matcher(parts[1]).matches()) {
                    method = false;
                    srg = parts[1];
                    readableName = members.get(obfuscatedClass + "|" + parts[0] + "|");
                    joinedFieldRows++;
                } else if (tsrg1 && parts.length == 3 && SRG_METHOD.matcher(parts[2]).matches()) {
                    method = true;
                    srg = parts[2];
                    readableName = members.get(obfuscatedClass + "|" + parts[0] + "|" + parts[1]);
                    joinedMethodRows++;
                } else if (parts.length == 3 && SRG_FIELD.matcher(parts[1]).matches()) {
                    method = false;
                    srg = parts[1];
                    readableName = members.get(obfuscatedClass + "|" + parts[0] + "|");
                    joinedFieldRows++;
                } else if (parts.length == 4 && SRG_METHOD.matcher(parts[2]).matches()) {
                    method = true;
                    srg = parts[2];
                    readableName = members.get(obfuscatedClass + "|" + parts[0] + "|" + parts[1]);
                    joinedMethodRows++;
                } else {
                    continue;
                }

                if (readableName == null) continue;
                String previous = output.putIfAbsent(srg, readableName);
                if (previous != null) {
                    if (!previous.equals(readableName)) {
                        throw new IOException("Conflicting SRG id mappings at " + tsrg + ":" + lineNumber
                                + " for " + srg + ": " + previous + " vs " + readableName);
                    }
                    duplicateSrgIds++;
                }
                if (method) {
                    matchedMethods++;
                } else {
                    matchedFields++;
                }
            }
        }

        if (output.isEmpty()) {
            throw new IOException("No SRG -> Mojang member mappings matched between " + tsrg + " and " + mojang);
        }
        Coverage coverage = new Coverage(
                mojangClassRows, mojangMethodRows, mojangFieldRows,
                joinedClassRows, joinedMethodRows, joinedFieldRows,
                matchedMethods, matchedFields, duplicateMemberKeys, duplicateSrgIds);
        // EXPERIMENTAL 1.16.5 scoping: MCP class name -> Mojang class name (only where they differ), for tsrg1 files
        Map<String, String> classRenames = new TreeMap<>();
        for (Map.Entry<String, String> e : classes.entrySet()) {
            String mcp = classNames.get(e.getValue());
            String mojangName = e.getKey().replace('.', '/');
            if (mcp != null && !mcp.equals(mojangName)) classRenames.put(mcp, mojangName);
        }
        return new GenerationResult(output, coverage, classRenames);
    }

    private static String desc(String type, Map<String, String> classes) {
        int dimensions = 0;
        while (type.endsWith("[]")) {
            dimensions++;
            type = type.substring(0, type.length() - 2);
        }
        String base = switch (type) {
            case "int" -> "I";
            case "long" -> "J";
            case "double" -> "D";
            case "float" -> "F";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "void" -> "V";
            default -> "L" + classes.getOrDefault(type, type.replace('.', '/')) + ";";
        };
        return "[".repeat(dimensions) + base;
    }
}
