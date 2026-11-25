package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * A wrong row of the 1.20.1 migration tables links and then misbehaves: {@code IronGolem.Crackiness} was mapped to a 1.21.1 class that exists but has none
 * of the enum's constants. This test checks every hand row against the committed 1.21.1 index ({@code mc-members.index.gz}, {@link McMembers1201}):
 * <ul>
 *   <li>a class row's target is a Minecraft/NeoForge class of 1.21.1 or a helper/shim class of this repository;</li>
 *   <li>the target of a class row has the kind of the old class (an enum stays an enum) and keeps most of its members, unless the row is listed with a
 *       reason in {@code legacyconnector/audit/mc-classes-shape-waivers.txt} (shapes of the old classes: {@code mc-classes-shapes.tsv}, written by
 *       {@code tools/dev/audit_1201_tables.py shapes} from Mojang's 1.20.1 mappings);</li>
 *   <li>a member row renames to a member that exists (on the target class or above it); a bridge row's new method exists with its descriptor;</li>
 *   <li>a call row that renames in place ({@code =}) names a method that exists, one that goes to a helper names a helper method that exists.</li>
 * </ul>
 */
class Mc1201TablesAuditTest {
    private static final Path SRC = Path.of("..", "src", "main", "java");
    private static final Set<String> IGNORED = Set.of("<init>", "<clinit>", "values", "valueOf", "$VALUES", "$values", "toString", "hashCode", "equals", "getClass",
            "name", "ordinal", "compareTo", "getMessage", "getDeclaringClass", "describeConstable", "wait", "notify", "notifyAll", "clone", "finalize");

    private final Mc1201Hierarchy hierarchy = new Mc1201Hierarchy();
    private final McHostIndex1201 index = new McHostIndex1201(hierarchy);

    private static List<String> lines(InputStream in) throws IOException {
        List<String> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String l = r.readLine(); l != null; l = r.readLine()) if (!l.isEmpty() && l.charAt(0) != '#') out.add(l);
        }
        return out;
    }

    private static List<String[]> rows(String file) throws IOException {
        List<String[]> out = new ArrayList<>();
        try (InputStream in = ForwardStage.D_1201.open(file)) {
            for (String l : lines(in)) out.add(l.split("\t", -1));
        }
        return out;
    }

    private static boolean repoHas(String internalName) {
        Path java = SRC.resolve(internalName.split("\\$")[0] + ".java");
        return Files.exists(java);
    }

    private Map<String, String> classMap() throws IOException {
        Map<String, String> m = new HashMap<>();
        for (String[] r : rows("mc-classes.tsv")) m.put(r[0], r[1]);
        return m;
    }

    private static String mapDesc(String desc, Map<String, String> cm) {
        StringBuilder sb = new StringBuilder();
        java.util.regex.Matcher m = Pattern.compile("L([^;]+);").matcher(desc);
        while (m.find()) m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("L" + cm.getOrDefault(m.group(1), m.group(1)) + ";"));
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean hasMethod(String owner, String name, String desc, Map<String, String> cm) {
        HostIndexLookup l = lookup(owner, name);
        if (l == null) return true; // not an indexed class (helper): nothing to compare with
        if (desc == null) return !l.chain.isEmpty() || !l.complete;
        return l.chain.contains(desc) || l.chain.contains(mapDesc(desc, cm)) || !l.complete;
    }

    private record HostIndexLookup(List<String> chain, boolean complete) {}

    private HostIndexLookup lookup(String owner, String name) {
        var l = index.methods(owner, name);
        return l == null ? null : new HostIndexLookup(l.chain(), l.chainComplete());
    }

    private boolean hasMember(String owner, String name, boolean field, Map<String, String> cm) {
        if (field) {
            Boolean b = index.hasField(owner, name);
            return b == null || b;
        }
        return hasMethod(owner, name, null, cm);
    }

    @Test
    void classRowsPointAtClassesThatExist() throws IOException {
        List<String> bad = new ArrayList<>();
        for (String[] r : rows("mc-classes.tsv")) {
            String n = r[1];
            if (McMembers1201.hasClass(n)) continue;
            if (Files.isDirectory(SRC) && (n.startsWith("org/kylepitcock/") || n.startsWith("legacyconnector/")) && repoHas(n)) continue;
            if (!Files.isDirectory(SRC) && (n.startsWith("org/kylepitcock/") || n.startsWith("legacyconnector/"))) continue; // repository sources not visible
            bad.add(r[0] + " -> " + n);
        }
        assertTrue(bad.isEmpty(), "mc-classes.tsv rows whose target is neither a 1.21.1 class nor a class of this repository:\n" + String.join("\n", bad));
    }

    @Test
    void classRowsKeepTheKindAndMostMembersOfTheOldClass() throws IOException {
        Set<String> waived = new HashSet<>();
        try (InputStream in = Mc1201TablesAuditTest.class.getResourceAsStream("/legacyconnector/audit/mc-classes-shape-waivers.txt")) {
            for (String l : lines(in)) waived.add(l.split("\t")[0].trim());
        }
        Map<String, String> cm = classMap();
        List<String> bad = new ArrayList<>();
        try (InputStream in = Mc1201TablesAuditTest.class.getResourceAsStream("/legacyconnector/audit/mc-classes-shapes.tsv")) {
            for (String l : lines(in)) {
                String[] p = l.split("\t", -1);
                String old = p[0];
                String neu = cm.get(old);
                if (neu == null || !McMembers1201.hasClass(neu) || waived.contains(old)) continue;
                boolean oldEnum = p[1].equals("enum");
                boolean newEnum = isEnum(neu);
                if (oldEnum != newEnum) {
                    bad.add(old + " -> " + neu + ": was " + p[1] + ", the target " + (newEnum ? "is an enum" : "is not an enum"));
                    continue;
                }
                Set<String> have = memberNames(neu);
                List<String> old1 = new ArrayList<>();
                for (String n : p.length > 2 ? p[2].split(",") : new String[0]) if (!n.isEmpty() && !IGNORED.contains(n)) old1.add(n);
                List<String> missing = new ArrayList<>();
                for (String n : old1) if (!have.contains(n)) missing.add(n);
                if (!old1.isEmpty() && missing.size() * 10 > old1.size() * 3) {
                    bad.add(old + " -> " + neu + ": " + missing.size() + " of " + old1.size() + " members of the old class are not on the target, e.g. "
                            + missing.subList(0, Math.min(6, missing.size())));
                }
            }
        }
        assertTrue(bad.isEmpty(), "mc-classes.tsv rows whose target has another shape than the old class (fix the row, or list the old class in "
                + "mc-classes-shape-waivers.txt with the reason it is fine):\n" + String.join("\n", bad));
    }

    private boolean isEnum(String c) {
        return hierarchy.supers(c).contains("java/lang/Enum");
    }

    private Set<String> memberNames(String c) {
        Set<String> out = new HashSet<>();
        collect(c, out, new HashSet<>());
        return out;
    }

    private void collect(String c, Set<String> out, Set<String> seen) {
        if (!seen.add(c)) return;
        McMembers1201.Members m = McMembers1201.get(c);
        if (m != null) {
            for (String x : m.methods()) out.add(x.substring(0, x.indexOf('(')));
            for (String x : m.fields()) out.add(x.substring(0, x.indexOf(':')));
        }
        for (String s : hierarchy.supers(c)) collect(s, out, seen);
    }

    @Test
    void memberRowsRenameToMembersThatExist() throws IOException {
        Map<String, String> cm = classMap();
        List<String> bad = new ArrayList<>();
        for (String[] r : rows("mc-members.tsv")) {
            String owner = cm.getOrDefault(r[0], r[0]);
            if (!McMembers1201.hasClass(owner)) continue;
            boolean field = r[2].startsWith(":");
            if (!hasMember(owner, r[3], field, cm)) bad.add(owner + " " + r[1] + " -> " + r[3]);
        }
        assertTrue(bad.isEmpty(), "mc-members.tsv rows whose new name does not exist on the target:\n" + String.join("\n", bad));
    }

    @Test
    void bridgeRowsDeclareMethodsThatExist() throws IOException {
        Map<String, String> cm = classMap();
        List<String> bad = new ArrayList<>();
        for (String[] r : rows("mc-bridges.tsv")) {
            String owner = cm.getOrDefault(r[0], r[0]);
            if (!McMembers1201.hasClass(owner)) continue;
            if (!hasMethod(owner, r[3], r[4], cm)) bad.add(owner + " " + r[3] + r[4]);
        }
        assertTrue(bad.isEmpty(), "mc-bridges.tsv rows whose 1.21.1 method does not exist:\n" + String.join("\n", bad));
    }

    @Test
    void inPlaceCallRowsAndHelperRowsPointAtMethodsThatExist() throws IOException {
        Map<String, String> cm = classMap();
        List<String> bad = new ArrayList<>();
        for (String[] r : rows("mc-calls.tsv")) {
            String owner = cm.getOrDefault(r[0], r[0]);
            String helper = r[3];
            if (helper.startsWith("=")) {
                String newName = helper.equals("=args") || r[4].equals("-") ? r[1] : r[4];
                String newDesc = r.length > 5 && r[5].startsWith("(") ? r[5] : null;
                if (McMembers1201.hasClass(owner) && !hasMethod(owner, newName, newDesc, cm)) {
                    bad.add("in place: " + owner + " " + r[1] + r[2] + " -> " + newName + (newDesc == null ? "" : newDesc));
                }
            } else if (!helper.equals("-") && Files.isDirectory(SRC)) {
                String method = r[4].equals("-") ? r[1] : r[4];
                Path java = SRC.resolve((helper.contains("/") ? helper : "org/kylepitcock/legacyconnector/v1_20_1/migrate/" + helper) + ".java");
                if (!Files.exists(java)) {
                    bad.add("helper class missing: " + helper);
                    continue;
                }
                String text = Files.readString(java);
                if (!method.equals("<init>") && !Pattern.compile("\\b" + Pattern.quote(method) + "\\s*\\(").matcher(text).find()) {
                    bad.add("helper method missing: " + helper + "." + method);
                }
            }
        }
        assertTrue(bad.isEmpty(), "mc-calls.tsv rows whose target does not exist:\n" + String.join("\n", bad));
    }
}
