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
            org.slf4j.LoggerFactory.getLogger("MCCompat").warn("Duplicate legacy registration of {} in {}; keeping the first", id, registry.key().location());
            return registry.get(id);
        }
        return Registry.register(registry, id, value);
    }

    private static boolean isFrozen(MappedRegistry<?> registry) {
        return registry.frozen; // made public by META-INF/accesstransformer.cfg
    }
}
