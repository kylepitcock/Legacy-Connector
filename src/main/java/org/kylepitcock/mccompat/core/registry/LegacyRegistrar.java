package org.kylepitcock.mccompat.core.registry;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.mccompat.core.LegacyLifecycle;

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
            org.slf4j.LoggerFactory.getLogger("Retrofit").warn("Duplicate legacy registration of {} in {}; keeping the first", id, registry.key().location());
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
                org.slf4j.LoggerFactory.getLogger("Retrofit").debug("Cannot inspect {} for orphaned legacy entries", registry.key(), e);
            }
        }
        return removed;
    }

    private static boolean isLegacyContent(Object value) {
        String n = value.getClass().getName();
        return n.startsWith("org.kylepitcock.mccompat.") || n.startsWith("mccompat.") || n.startsWith("mccompat_synth.");
    }

    private static boolean isFrozen(MappedRegistry<?> registry) {
        return registry.frozen; // made public by META-INF/accesstransformer.cfg
    }
}
