package org.kylepitcock.legacyconnector.core.registry;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;

/**
 * All legacy registrations go through here. Legacy mods register imperatively (GameRegistry.registerBlock in
 * preInit or even init), so the whole legacy loading sequence runs inside NeoForge's registration window,
 * while every built-in registry is unfrozen, and entries are written straight into the registries.
 */
public final class LegacyRegistrar {
    private LegacyRegistrar() {}

    public static <T> T register(Registry<T> registry, ResourceLocation id, T value) {
        if (!LegacyLifecycle.isRegistrationOpen() && registry instanceof MappedRegistry<?> mapped && isFrozen(mapped)) {
            throw new IllegalStateException("Legacy mod registered " + id + " into " + registry.key().location()
                    + " after registries were frozen. Legacy registrations are only possible during preInit/init/postInit.");
        }
        if (registry.containsKey(id)) {
            // Legacy FML tolerated (and warned about) duplicate names; the modern registry would throw.
            org.slf4j.LoggerFactory.getLogger("Legacy Connector").warn("Duplicate legacy registration of {} in {}; keeping the first (dropped {})", id, registry.key().location(),
                    value instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemHolder h && h.legacyItem() != null ? h.legacyItem().getClass().getName() : value.getClass().getName());
            return registry.get(id);
        }
        return Registry.register(registry, id, value);
    }

    /**
     * Blocks/items a legacy mod constructed but never got registered (the mod failed half-way, or registered a
     * duplicate name) still own an "intrusive holder", and 1.21 refuses to freeze a registry with orphans. Drop the
     * orphans that belong to legacy content once legacy loading is over.
     */
    public static int discardOrphans() {
        int removed = 0;
        for (Registry<?> registry : net.minecraft.core.registries.BuiltInRegistries.REGISTRY) {
            if (!(registry instanceof MappedRegistry<?> mapped)) continue;
            try {
                java.lang.reflect.Field f = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
                f.setAccessible(true);
                java.util.Map<?, ?> orphans = (java.util.Map<?, ?>) f.get(mapped);
                if (orphans == null) continue;
                var it = orphans.keySet().iterator();
                while (it.hasNext()) {
                    Object value = it.next();
                    if (isLegacyContent(value)) {
                        it.remove();
                        removed++;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger("Legacy Connector").debug("Cannot inspect {} for orphaned legacy entries", registry.key(), e);
            }
        }
        return removed;
    }

    /**
     * Writes the keys of the legacy blocks, items and mob effects to {@code .legacyconnector/registry-keys.txt}. A client that the server
     * kicks with "registries with unknown keys" and that server each write one; a diff of the two names the differing keys.
     */
    public static void dumpLegacyKeys() {
        try {
            java.util.TreeSet<String> lines = new java.util.TreeSet<>();
            for (Registry<?> registry : java.util.List.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM, net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT)) {
                for (var e : registry.entrySet()) {
                    if (isLegacyContent(e.getValue())) lines.add(registry.key().location().getPath() + " " + e.getKey().location());
                }
            }
            java.nio.file.Path file = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve(".legacyconnector").resolve("registry-keys.txt");
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.write(file, lines);
            org.slf4j.LoggerFactory.getLogger("Legacy Connector").info("Wrote {} legacy registry keys to {}", lines.size(), file);
        } catch (java.io.IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("Legacy Connector").debug("Could not write the legacy registry key list", e);
        }
    }

    private static boolean isLegacyContent(Object value) {
        String n = value.getClass().getName();
        return n.startsWith("org.kylepitcock.legacyconnector.") || n.startsWith("legacyconnector.") || n.startsWith("legacyconnector_synth.");
    }

    private static boolean isFrozen(MappedRegistry<?> registry) {
        return registry.frozen; // made public by META-INF/accesstransformer.cfg
    }
}
