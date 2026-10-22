package org.kylepitcock.legacyconnector.core;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/**
 * Identifier normalization shared by all shims. Must stay in sync with the loader's
 * {@code LegacyModDecl.toModernId} and {@code LegacyResourceConverter.normalizePath}: the loader renames files and
 * mod ids with these rules, the runtime renames references with them.
 */
public final class Ids {
    private Ids() {}

    public static String modernModId(String legacyId) {
        StringBuilder sb = new StringBuilder();
        for (char c : legacyId.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' ? c : '_');
        }
        if (sb.isEmpty() || !(sb.charAt(0) >= 'a' && sb.charAt(0) <= 'z')) sb.insert(0, "mod_");
        while (sb.length() < 2) sb.append('_');
        if (sb.length() > 64) sb.setLength(64);
        return sb.toString();
    }

    public static String normalizePath(String path) {
        StringBuilder sb = new StringBuilder(path.length());
        for (char c : path.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-' ? c : '_');
        }
        return sb.toString();
    }

    /** Namespace for a legacy resource domain: domains are lower-cased mod ids in legacy code. */
    public static String namespace(String legacyDomain) {
        String ns = normalizePath(legacyDomain).replace('/', '_').replace('.', '_');
        return ns.isEmpty() ? "minecraft" : ns;
    }

    /** {@code "domain:path"} or {@code "path"} (minecraft) to a modern location. */
    public static ResourceLocation location(String legacy, String defaultNamespace) {
        int colon = legacy.indexOf(':');
        String ns = colon < 0 ? defaultNamespace : namespace(legacy.substring(0, colon));
        String path = normalizePath(colon < 0 ? legacy : legacy.substring(colon + 1));
        if (path.isEmpty()) path = "unnamed";
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }
}
