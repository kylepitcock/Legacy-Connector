package org.kylepitcock.legacyconnector.core.host;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The one place that reads a value out of a modern registry by id. Registry.get(ResourceLocation) returns the value on 1.21.1
 * (the default value for a defaulted registry, null for a plain one); later host versions return an Optional of a holder and
 * name the value accessor differently, so a host bump changes only this class. See docs/wiki/Host-Version-Upgrades.md.
 */
public final class HostRegistries {
    private HostRegistries() {}

    public static <T> T get(Registry<T> registry, ResourceLocation id) {
        return registry.get(id);
    }

    public static <T> T get(Registry<T> registry, ResourceKey<T> key) {
        return registry.get(key);
    }
}
