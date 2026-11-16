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
        /** official class to its Calamus intermediary name, and back: intermediary names are stable across versions for what did not change */
        final Map<String, String> interOf = new HashMap<>();
        final Map<String, String> officialByInter = new HashMap<>();
        /** {@code kind|ownerInter|memberInter} to {officialOwner, officialName, officialDesc} */
        final Map<String, String[]> byInter = new HashMap<>();

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
            n.interOf.put(c.from, c.to);
            n.officialByInter.put(c.to, c.from);
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
                n.members.add(new String[] {r.kind, e.getKey(), r.from, r.desc, key, ownerInter, r.to});
                n.byInter.putIfAbsent(r.kind + "|" + ownerInter + "|" + r.to, new String[] {e.getKey(), r.from, r.desc});
            }
        }
        return n;
    }

    /** {@code kind|readable name} of every member of an official class. */
    static java.util.Set<String> memberNames(Names n, String official) {
        java.util.Set<String> out = new java.util.HashSet<>();
        String owner = n.classes.get(official);
        for (String[] m : n.members) if (m[1].equals(official)) out.add(m[0] + "|" + memberName(m, owner));
        return out;
    }

    /** The readable member name of a {@link Names#members} row (its key is {@code owner.name desc}). */
    static String memberName(String[] row, String ownerReadable) {
        return row[4].substring(ownerReadable.length() + 1, row[4].indexOf(' ', ownerReadable.length()));
    }

    /** A descriptor with every class type reduced to {@code L;}: two signatures that differ only in the classes (a field typed World in 1.2.5, WorldClient in 1.3). */
    static String erase(String desc) {
        return desc.replaceAll("L[^;]*;", "L;");
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
        // 1.4.7 members by owner, kind and readable name: the fallback for members whose descriptor changed in a game class type (see the join in main)
        Map<String, String> read147 = new HashMap<>(); // official owner|name|desc of 1.4.7 to its readable name
        for (String[] m : v147.members) read147.put(m[1] + "|" + m[2] + "|" + m[3], memberName(m, v147.classes.get(m[1])));
        StringBuilder nameChanges = new StringBuilder(), rejectedChanges = new StringBuilder(), classChanges = new StringBuilder(), classRejected = new StringBuilder();
        final char T = (char) 9, LF = (char) 10;
        java.util.Set<String> reviewedClasses = new java.util.HashSet<>();
        try (InputStream in = Mc125Mappings.class.getResourceAsStream("/legacyconnector/mappings/1.2.5/reviewed-class-pairs.tsv")) {
            if (in != null) for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("" + LF)) if (!line.startsWith("#") && !line.isBlank()) reviewedClasses.add(line.replace("" + (char) 13, ""));
        }
        java.util.Set<String> reviewed = new java.util.HashSet<>();
        try (InputStream in = Mc125Mappings.class.getResourceAsStream("/legacyconnector/mappings/1.2.5/reviewed-name-changes.tsv")) {
            if (in != null) for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) if (!line.startsWith("#") && !line.isBlank()) reviewed.add(line.replace("\r", ""));
        }
        Map<String, String> aliases = new HashMap<>(); // "kind owner.name125" to name147 (manual-aliases.tsv)
        try (InputStream in = Mc125Mappings.class.getResourceAsStream("/legacyconnector/mappings/1.2.5/manual-aliases.tsv")) {
            if (in != null) {
                for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                    String[] p = line.replace("\r", "").split("\t");
                    if (p.length == 3 && p[0].equals("s")) aliases.put("s " + p[1], p[2]); // a class of the shim with a name of its own
                    else if (p.length == 4 && p[0].equals("s")) { aliases.put("s " + p[1], p[2]); aliases.put("S " + p[1], p[3]); } // ... and the readable 1.2.5 class whose supertypes it has
                    else if (p.length == 4 && !line.startsWith("#")) aliases.put(p[0] + " " + p[1] + "." + p[2], p[3]);
                    else if (p.length == 3 && p[0].equals("c")) aliases.put("c " + p[1], p[2]); // a class that took over another one's role
                }
            }
        }
        Map<String, List<String[]>> by147 = new HashMap<>();
        for (String[] m : v147.members) {
            String owner = v147.classes.get(m[1]);
            by147.computeIfAbsent(m[0] + " " + m[1] + "|" + memberName(m, owner), k -> new ArrayList<>()).add(m);
        }
        Path out = Path.of(args[6]);
        Files.createDirectories(tableFile(out).getParent());
        StringBuilder report = new StringBuilder("1.2.5 to 1.4.7 join (class, member name and descriptor in Feather names)\n");
        try (BufferedWriter table = Files.newBufferedWriter(tableFile(out), StandardCharsets.UTF_8)) {
            String[] sides = {"c", "s"};
            for (int s = 0; s < 2; s++) {
                Names v125 = load(Path.of(args[s * 2]), Path.of(args[s * 2 + 1]));
                int classesJoined = 0, membersJoined = 0, membersTotal = 0, joinedByErasure = 0, joinedByName = 0, joinedByInter = 0, joinedByAlias = 0;
                Map<String, int[]> areaClasses = new TreeMap<>(); // area -> {classes, joined, members, joined members}
                Map<String, List<String>> lostClasses = new TreeMap<>();
                Map<String, String> class147 = new HashMap<>(), shimClasses = new HashMap<>();
                for (var e : v125.classes.entrySet()) {
                    String readable = e.getValue();
                    String off147 = readableTo147.get(readable);
                    if (off147 == null) {
                        // same Calamus intermediary id, other readable name or package. Intermediary ids are reused for other things (15 of 20 member joins by id were wrong), so a class pair is accepted
                        // only when the members confirm it: at least half of the 1.2.5 class's readable members exist by name in the 1.4.7 class (all of them for a class of at most two members),
                        // or somebody reviewed the pair (reviewed-class-pairs.tsv)
                        String cand = v147.officialByInter.get(v125.interOf.get(e.getKey()));
                        if (cand != null) {
                            java.util.Set<String> mine = memberNames(v125, e.getKey()), theirs = memberNames(v147, cand);
                            long common = mine.stream().filter(theirs::contains).count();
                            double share = mine.isEmpty() ? 1.0 : common / (double) mine.size();
                            String pair = readable + T + v147.classes.get(cand);
                            boolean ok = mine.size() <= 2 ? common == mine.size() : share >= 0.5;
                            if (reviewedClasses.contains(pair)) {
                                off147 = cand;
                                classChanges.append(sides[s]).append(T).append(pair).append(T).append(Math.round(share * 100)).append(T).append("reviewed").append(LF);
                            } else {
                                // a plausible pair nobody reviewed yet ("unreviewed": the audit test fails until it is looked at and added to reviewed-class-pairs.tsv), or one the members do not confirm ("weak")
                                classRejected.append(sides[s]).append(T).append(pair).append(T).append(Math.round(share * 100)).append(T).append(ok ? "unreviewed" : "weak").append(LF);
                            }
                        }
                    }
                    // a class the shim has under a name of its own (BossEntity is net/minecraft/src/EntityDragonBase): the members are looked up there by their Feather names
                    if (off147 == null && aliases.containsKey("s " + readable)) {
                        off147 = aliases.get("s " + readable);
                        shimClasses.put(e.getKey(), off147);
                        String superReadable = aliases.get("S " + readable);
                        if (superReadable != null) {
                            for (var other : v125.classes.entrySet()) {
                                if (other.getValue().equals(superReadable)) table.write("S" + T + sides[s] + T + e.getKey() + T + other.getKey() + LF);
                            }
                        }
                    }
                    if (off147 == null && aliases.containsKey("c " + readable)) off147 = readableTo147.get(aliases.get("c " + readable)); // curated: the class that took the role over
                    int[] a = areaClasses.computeIfAbsent(area(readable), k -> new int[4]);
                    a[0]++;
                    if (off147 == null) {
                        lostClasses.computeIfAbsent(area(readable), k -> new ArrayList<>()).add(readable);
                        continue;
                    }
                    a[1]++;
                    classesJoined++;
                    class147.put(e.getKey(), off147);
                    table.write("C\t" + sides[s] + "\t" + e.getKey() + "\t" + off147 + "\n");
                }
                java.util.Set<String> joinedKeys = new java.util.HashSet<>(), usedTargets = new java.util.HashSet<>();
                for (String[] m : v125.members) {
                    membersTotal++;
                    String ownerReadable = v125.classes.get(m[1]);
                    int[] a = areaClasses.computeIfAbsent(area(ownerReadable), k -> new int[4]);
                    a[2]++;
                    boolean method = m[0].equals("m");
                    String[] to = shimClasses.containsKey(m[1]) ? new String[] {shimClasses.get(m[1]), memberName(m, ownerReadable), mapDesc(m[3], class147)} : (method ? v147.methods : v147.fields).get(m[4]);
                    if (to == null && class147.containsKey(m[1])) {
                        // the same member as in 1.4.7 under another readable name or with other class types: Calamus gave it the same intermediary name in both versions
                        to = v147.byInter.get(m[0] + "|" + m[5] + "|" + m[6]);
                        if (to != null && !erase(to[2]).equals(erase(m[3]))) to = null; // other primitives or array depth: not the same member
                        if (to != null) {
                            String n147 = read147.get(to[0] + "|" + to[1] + "|" + to[2]);
                            String n125 = memberName(m, ownerReadable);
                            // audit: the same intermediary id under two Feather names is sometimes a rename Feather made between builds (Item.MAP, FILLED_MAP) and sometimes a reused id for another
                            // member (PlayerManager.unban joined to removeFromWhitelist); only the reviewed ones (reviewed-name-changes.tsv) are joined, the rest is left to the other tiers and listed
                            if (n147 != null && !n147.equals(n125)) {
                                String line = ownerReadable + "\t" + n125 + "\t" + n147;
                                if (reviewed.contains(line)) {
                                    nameChanges.append(sides[s]).append("\t").append(line).append("\t").append(m[3]).append("\n");
                                } else {
                                    rejectedChanges.append(sides[s]).append("\t").append(line).append("\t").append(m[3]).append("\t").append(m[1]).append("\t").append(m[2]).append("\n");
                                    to = null;
                                }
                            }
                            if (to != null) joinedByInter++;
                        }
                    }
                    if (to == null && class147.containsKey(m[1])) {
                        // a member 1.4.7 has under another name, curated by hand
                        String alias = aliases.get(m[0] + " " + ownerReadable + "." + memberName(m, ownerReadable));
                        List<String[]> cands = alias == null ? null : by147.get(m[0] + " " + class147.get(m[1]) + "|" + alias);
                        if (cands != null) {
                            List<String[]> same = cands.stream().filter(c -> erase(c[3]).equals(erase(m[3]))).toList();
                            if (same.size() == 1) {
                                to = new String[] {same.get(0)[1], same.get(0)[2], same.get(0)[3]};
                                joinedByAlias++;
                            }
                        }
                    }
                    if (to == null) {
                        // the same member with other class types in its descriptor (1.3 retyped Minecraft.theWorld/thePlayer, the integrated server, entity and item classes):
                        // same name and the descriptor equal up to class names, else the only member of that name in the owner
                        List<String[]> cands = by147.get(m[0] + " " + class147.get(m[1]) + "|" + memberName(m, ownerReadable));
                        if (cands != null && class147.containsKey(m[1])) {
                            String wanted = erase(m[3]);
                            List<String[]> same = cands.stream().filter(c -> erase(c[3]).equals(wanted)).toList();
                            String[] pick = same.size() == 1 ? same.get(0) : null;
                            if (pick != null) joinedByErasure++;
                            else if (cands.size() == 1 && !method) {
                                pick = cands.get(0); // a field is unique by name in its class
                                joinedByName++;
                            }
                            if (pick != null) to = new String[] {pick[1], pick[2], pick[3]};
                        }
                    }
                    if (to == null) continue;
                    a[3]++;
                    membersJoined++;
                    joinedKeys.add(m[1] + "|" + m[0] + "|" + m[2] + "|" + m[3]);
                    usedTargets.add(to[0] + "|" + m[0] + "|" + to[1] + "|" + to[2]);
                    table.write((method ? "M" : "F") + "\t" + sides[s] + "\t" + m[1] + "\t" + m[2] + "\t" + m[3] + "\t" + to[0] + "\t" + to[1] + "\t" + to[2] + "\n");
                }
                // what is left of a joined class: the one unjoined 1.2.5 member and the one unclaimed 1.4.7 member of a kind and class-erased descriptor. Reported only: checked
                // by hand on 1.2.5 they are mostly NOT the same member (stuckArrows/armSwingingTicks), so they are never joined
                Map<String, List<String[]>> left125 = new TreeMap<>(), left147 = new TreeMap<>();
                for (String[] m : v125.members) {
                    String to147 = class147.get(m[1]);
                    if (to147 == null || joinedKeys.contains(m[1] + "|" + m[0] + "|" + m[2] + "|" + m[3])) continue;
                    left125.computeIfAbsent(m[1] + "|" + m[0] + "|" + erase(m[3]), k -> new ArrayList<>()).add(m);
                }
                for (String[] m : v147.members) {
                    if (usedTargets.contains(m[1] + "|" + m[0] + "|" + m[2] + "|" + m[3])) continue;
                    left147.computeIfAbsent(m[1] + "|" + m[0] + "|" + erase(m[3]), k -> new ArrayList<>()).add(m);
                }
                int joinedByLeftover = 0;
                StringBuilder pairs = new StringBuilder();
                for (var e : left125.entrySet()) {
                    String[] k = e.getKey().split("\\|", 3);
                    String to147 = class147.get(k[0]);
                    List<String[]> cand = left147.get(to147 + "|" + k[1] + "|" + k[2]);
                    if (e.getValue().size() != 1 || cand == null || cand.size() != 1) continue;
                    String[] m = e.getValue().get(0), t = cand.get(0);
                    if (m[2].length() > 3 && m[2].equals(t[2])) continue;
                    joinedByLeftover++;
                    pairs.append("  ").append(v125.classes.get(m[1])).append(": ").append(memberName(m, v125.classes.get(m[1]))).append(" ").append(m[3]).append(" -> ")
                            .append(memberName(t, v147.classes.get(t[1]))).append("\n");
                }
                // U rows: every member that stayed unjoined, with its readable (Feather) name: the obfuscated letter differs between the client and the server of 1.2.5, the readable name does not
                for (String[] m : v125.members) {
                    if (joinedKeys.contains(m[1] + "|" + m[0] + "|" + m[2] + "|" + m[3])) continue;
                    String ownerReadable = v125.classes.get(m[1]);
                    table.write("U\t" + sides[s] + "\t" + m[1] + "\t" + m[2] + "\t" + m[3] + "\t" + memberName(m, ownerReadable) + "\n");
                }
                // B rows: a method of a joined class that 1.4.7 has under the same readable name but another signature (1.3 added the hit position to onItemUse, ...). Not a rename of
                // calls (the call sites cannot be given the new arguments); a mod class that DECLARES the old signature gets the 1.4.7 signature as a bridge method (OverrideBridges).
                int bridgeRows = 0, bridgeSkipped = 0;
                StringBuilder bridgeReport = new StringBuilder();
                for (String[] m : v125.members) {
                    if (!m[0].equals("m") || !class147.containsKey(m[1]) || joinedKeys.contains(m[1] + "|" + m[0] + "|" + m[2] + "|" + m[3])) continue;
                    String ownerReadable = v125.classes.get(m[1]);
                    List<String[]> cands = by147.get("m " + class147.get(m[1]) + "|" + memberName(m, ownerReadable));
                    String aliasName = aliases.get("m " + ownerReadable + "." + memberName(m, ownerReadable));
                    if (aliasName != null) { // a rename curated from evidence (manual-aliases.tsv): its candidates join the ones of the same name, and the argument list may differ
                        List<String[]> extra = by147.get("m " + class147.get(m[1]) + "|" + aliasName);
                        if (extra != null) {
                            cands = new ArrayList<>(cands == null ? List.of() : cands);
                            cands.addAll(extra);
                        }
                    }
                    if (cands == null) continue;
                    String oldDesc = v125.readableDesc(m[3]);
                    String[] bestRow = null;
                    int bestScore = Integer.MIN_VALUE;
                    boolean tie = false;
                    String[] reason = new String[1];
                    for (String[] c : cands) {
                        if (!c[1].equals(class147.get(m[1])) || usedTargets.contains(c[1] + "|m|" + c[2] + "|" + c[3])) continue;
                        org.kylepitcock.legacyconnector.loader.transform.OverrideBridges.Plan plan = org.kylepitcock.legacyconnector.loader.transform.OverrideBridges.plan(oldDesc, v147.readableDesc(c[3]), reason);
                        if (plan == null) continue;
                        if (plan.score() > bestScore) {
                            bestScore = plan.score();
                            bestRow = c;
                            tie = false;
                        } else if (plan.score() == bestScore) {
                            tie = true;
                        }
                    }
                    if (bestRow == null || tie) {
                        bridgeSkipped++;
                        continue;
                    }
                    bridgeRows++;
                    table.write("B\t" + sides[s] + "\t" + m[1] + "\t" + m[2] + "\t" + m[3] + "\t" + bestRow[1] + "\t" + bestRow[2] + "\t" + bestRow[3] + "\n");
                    bridgeReport.append("  ").append(ownerReadable).append(": ").append(memberName(m, ownerReadable)).append(' ').append(oldDesc).append(" -> ").append(v147.readableDesc(bestRow[3])).append("\n");
                }
                report.append("\n== ").append(s == 0 ? "client" : "server").append(": ").append(bridgeRows).append(" bridge rows (override signatures changed between versions), ")
                        .append(bridgeSkipped).append(" ambiguous or unbridgeable\n").append(bridgeReport);
                report.append("\n== ").append(s == 0 ? "client" : "server").append(": classes joined ").append(classesJoined).append(" of ").append(v125.classes.size())
                        .append(", members joined ").append(membersJoined).append(" of ").append(membersTotal).append(" (").append(joinedByErasure)
                        .append(" by name with the class types of the descriptor ignored, ").append(joinedByName).append(" fields by name only, ").append(joinedByInter)
                        .append(" by the same intermediary name, ").append(joinedByLeftover).append(" leftover candidates, NOT joined: see the report, ").append(joinedByAlias).append(" by manual alias)\n");
                report.append("leftover candidates (unique by kind and descriptor; not joined, mostly different members):\n").append(pairs);
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
        Files.writeString(out.resolve("join-name-changes.tsv"), nameChanges.toString(), StandardCharsets.UTF_8);
        Files.writeString(out.resolve("join-class-changes.tsv"), classChanges.toString(), StandardCharsets.UTF_8);
        Files.writeString(out.resolve("join-class-rejected.tsv"), classRejected.toString(), StandardCharsets.UTF_8);
        Files.writeString(out.resolve("join-name-rejected.tsv"), rejectedChanges.toString(), StandardCharsets.UTF_8);
        System.out.println("1.2.5 to 1.4.7 join written to " + out.toAbsolutePath());
        System.out.print(report.substring(0, Math.min(report.length(), 600)));
    }
}
