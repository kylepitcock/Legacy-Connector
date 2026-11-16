package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Collects references a transformed jar makes that will not link at runtime.
 *
 * <p>This is the single most useful diagnostic the layer produces: it predicts which legacy APIs a mod
 * needs that the shim does not (yet) implement, before the game crashes with a NoSuchMethodError.</p>
 */
public final class LinkReport {
    /** Shim classes the mod needs that do not exist. Value: referencing classes. */
    public final Map<String, TreeSet<String>> missingClasses = new TreeMap<>();
    /** {@code owner.member} -> referencing classes, for shim classes that exist but lack the member. */
    public final Map<String, TreeSet<String>> missingShimMembers = new TreeMap<>();
    /** Library/JDK members removed since the mod was built (e.g. old Guava APIs). */
    public final Map<String, TreeSet<String>> missingLibraryMembers = new TreeMap<>();
    /** Legacy API members the shim lacks that were synthesized (they link, but do nothing yet). Informational. */
    public final Map<String, TreeSet<String>> stubbedMembers = new TreeMap<>();
    /** Calls whose signature differs from the shim's (another return type, another constructor) and were adapted to it: real behaviour. */
    public final Map<String, TreeSet<String>> adaptedMembers = new TreeMap<>();
    /** Legacy API classes the shim lacks, generated with their legacy supertypes. Informational. */
    public final java.util.Set<String> synthesizedClasses = new TreeSet<>();
    /** Classes found nowhere: usually optional integrations with other mods. Informational. */
    public final Map<String, TreeSet<String>> externalClasses = new TreeMap<>();

    /** {@code owner.nameDesc} of @Optional.Method members removed for an absent mod; references to them are guarded integration code, not defects. */
    public final java.util.Set<String> optionalRemoved = new java.util.HashSet<>();
    /** Calls to members removed because their {@code @Optional.Method} mod is absent: "optional integration not available". Informational. */
    public final Map<String, TreeSet<String>> optionalMembers = new TreeMap<>();

    /** Set by the linker: true when a class (or an ancestor) lost an {@code @Optional.Interface}, so members it declared may be missing on purpose. */
    java.util.function.Predicate<String> lostOptionalInterface = c -> false;

    void missingClass(String owner, String from) {
        missingClasses.computeIfAbsent(owner, k -> new TreeSet<>()).add(from);
    }

    void missingMember(String owner, String member, String from, boolean shim) {
        if (optionalRemoved.contains(owner + "." + member) || lostOptionalInterface.test(owner)) {
            optionalMembers.computeIfAbsent(owner + "." + member, k -> new TreeSet<>()).add(from);
            return;
        }
        (shim ? missingShimMembers : missingLibraryMembers).computeIfAbsent(owner + "." + member, k -> new TreeSet<>()).add(from);
    }

    void stubbed(String owner, String member, String from) {
        stubbedMembers.computeIfAbsent(owner + "." + member, k -> new TreeSet<>()).add(from);
    }

    void adapted(String owner, String member, String from) {
        adaptedMembers.computeIfAbsent(owner + "." + member, k -> new TreeSet<>()).add(from);
    }

    void externalClass(String owner, String from) {
        externalClasses.computeIfAbsent(owner, k -> new TreeSet<>()).add(from);
    }

    public int errorCount() {
        return missingClasses.size() + missingShimMembers.size() + missingLibraryMembers.size();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        if (!synthesizedClasses.isEmpty()) {
            sb.append("  Synthesized ").append(synthesizedClasses.size()).append(" legacy API class(es) the shim lacks (they link, behaviour is default)\n");
            for (String c : synthesizedClasses) sb.append("    - ").append(c.replace('/', '.')).append('\n');
        }
        section(sb, "Missing shim classes (legacy API not implemented yet)", missingClasses);
        section(sb, "Missing shim members", missingShimMembers);
        section(sb, "Removed library/JDK members", missingLibraryMembers);
        section(sb, "Synthesized legacy APIs (link, but have no behaviour yet)", stubbedMembers);
        section(sb, "Adapted to the shim's signature (real behaviour)", adaptedMembers);
        section(sb, "Optional integrations not available (their @Optional.Method mod is absent)", optionalMembers);
        section(sb, "Unresolved external classes (optional mod integrations, usually harmless)", externalClasses);
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, Map<String, TreeSet<String>> entries) {
        if (entries.isEmpty()) return;
        sb.append("  ").append(title).append(" [").append(entries.size()).append("]\n");
        for (var e : entries.entrySet()) {
            sb.append("    - ").append(e.getKey().replace('/', '.'));
            sb.append("   <- ").append(e.getValue().first().replace('/', '.'));
            if (e.getValue().size() > 1) sb.append(" (+").append(e.getValue().size() - 1).append(" more)");
            sb.append('\n');
        }
    }
}
