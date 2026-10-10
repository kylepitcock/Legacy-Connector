package org.kylepitcock.mccompat.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One {@code @Mod}-annotated class found in a legacy jar, merged with its mcmod.info entry (if any).
 */
public final class LegacyModDecl {
    /** The mod id exactly as the legacy mod declares it (may contain upper case, '|' and so on). */
    public String legacyId;
    /** Internal name of the {@code @Mod} class, before relocation (mod classes are never relocated). */
    public String mainClass;
    public String name;
    public String version;
    public String description = "";
    public final List<String> authors = new ArrayList<>();
    public String url;
    public String logoFile;
    public String credits;
    /** FML dependency string, e.g. {@code required-after:Forge@[10.13.4,);after:NotEnoughItems}. */
    public String dependencies = "";
    public String acceptedMinecraftVersions;
    public boolean clientSideOnly;
    public boolean serverSideOnly;

    /**
     * NeoForge requires mod ids to match {@code [a-z][a-z0-9_]{1,63}}. Legacy ids like {@code BuildCraft|Core}
     * or {@code IC2} do not, so they are normalized; the runtime keeps a legacy -> modern table so
     * {@code Loader.isModLoaded("BuildCraft|Core")} keeps working.
     */
    public String modernId() {
        return toModernId(legacyId);
    }

    public static String toModernId(String legacyId) {
        StringBuilder sb = new StringBuilder();
        for (char c : legacyId.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' ? c : '_');
        }
        if (sb.isEmpty() || !(sb.charAt(0) >= 'a' && sb.charAt(0) <= 'z')) sb.insert(0, "mod_");
        while (sb.length() < 2) sb.append('_');
        if (sb.length() > 64) sb.setLength(64);
        return sb.toString();
    }

    /** Legacy ids of mods this one must load after ({@code required-after}, {@code after}). */
    public List<Dependency> parsedDependencies() {
        List<Dependency> out = new ArrayList<>();
        if (dependencies == null || dependencies.isBlank()) return out;
        for (String part : dependencies.split(";")) {
            part = part.trim();
            int colon = part.indexOf(':');
            if (colon < 0) continue;
            String kind = part.substring(0, colon).trim();
            String target = part.substring(colon + 1).trim();
            String range = null;
            int at = target.indexOf('@');
            if (at >= 0) {
                range = target.substring(at + 1);
                target = target.substring(0, at);
            }
            if (target.equals("*")) continue;
            boolean required = kind.startsWith("required-");
            String ordering = kind.endsWith("before") ? "BEFORE" : kind.endsWith("after") ? "AFTER" : "NONE";
            out.add(new Dependency(target, required, ordering, range));
        }
        return out;
    }

    public record Dependency(String legacyId, boolean required, String ordering, String versionRange) {
        /** Forge/FML/Minecraft themselves are provided by NeoForge + the shim. */
        public boolean isPlatform() {
            String id = legacyId.toLowerCase(Locale.ROOT);
            return id.equals("forge") || id.equals("fml") || id.equals("minecraft") || id.equals("mcp") || id.equals("forgemultipartapi");
        }
    }

    @Override
    public String toString() {
        return legacyId + " (" + mainClass + ")";
    }
}
