package org.kylepitcock.legacyconnector.api.internal;

import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.api.LegacyVersion;
import org.kylepitcock.legacyconnector.core.LegacyMods;

/**
 * Which registry namespaces belong to which legacy version. Forge 1.20.1 mods are not in {@link LegacyMods} (they are ordinary NeoForge
 * mods after the in-place remap); the loader hands their ids over in a system property that {@code Forge1201Runtime} parses.
 */
public final class InPlaceMods {
    private InPlaceMods() {}

    /** The mod ids of the translated Forge 1.20.1 mods (sorted); empty when there are none. */
    public static Set<String> ids() {
        return new TreeSet<>(org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime.modIds());
    }

    public static boolean isInPlaceNamespace(String namespace) {
        return org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime.modIds().contains(namespace);
    }

    public static boolean isInPlace(ResourceLocation id) {
        return id != null && isInPlaceNamespace(id.getNamespace());
    }

    private static volatile java.util.Map<String, LegacyVersion> byNamespace;

    /** The legacy version of the mod that owns a registry namespace, empty for vanilla and modern mods. */
    public static java.util.Optional<LegacyVersion> versionOfNamespace(String namespace) {
        if (isInPlaceNamespace(namespace)) return java.util.Optional.of(LegacyVersion.V1_20_1);
        java.util.Map<String, LegacyVersion> map = byNamespace;
        if (map == null) {
            var all = LegacyMods.all();
            if (all.isEmpty()) return java.util.Optional.empty(); // not discovered yet: do not cache
            map = new java.util.HashMap<>();
            for (var m : all) {
                var v = LegacyVersion.byId(m.version);
                if (v.isPresent()) map.put(m.modernId, v.get());
            }
            byNamespace = map;
        }
        return java.util.Optional.ofNullable(map.get(namespace));
    }
}
