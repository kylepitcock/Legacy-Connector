package org.kylepitcock.mccompat.loader.transform;

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
    /** Classes found nowhere: usually optional integrations with other mods. Informational. */
    public final Map<String, TreeSet<String>> externalClasses = new TreeMap<>();

    void missingClass(String owner, String from) {
        missingClasses.computeIfAbsent(owner, k -> new TreeSet<>()).add(from);
    }

    void missingMember(String owner, String member, String from, boolean shim) {
        (shim ? missingShimMembers : missingLibraryMembers).computeIfAbsent(owner + "." + member, k -> new TreeSet<>()).add(from);
    }

    void externalClass(String owner, String from) {
        externalClasses.computeIfAbsent(owner, k -> new TreeSet<>()).add(from);
    }

    public int errorCount() {
        return missingClasses.size() + missingShimMembers.size() + missingLibraryMembers.size();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        section(sb, "Missing shim classes (legacy API not implemented yet)", missingClasses);
        section(sb, "Missing shim members", missingShimMembers);
        section(sb, "Removed library/JDK members", missingLibraryMembers);
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
