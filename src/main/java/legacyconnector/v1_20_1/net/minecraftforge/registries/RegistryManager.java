package legacyconnector.v1_20_1.net.minecraftforge.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1's registry manager: {@code ACTIVE} answers registries by name. */
public final class RegistryManager {
    public static final RegistryManager ACTIVE = new RegistryManager("active");
    public static final RegistryManager VANILLA = new RegistryManager("vanilla");
    public static final RegistryManager FROZEN = new RegistryManager("frozen");
    private final String name;

    private RegistryManager(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <V> IForgeRegistry<V> getRegistry(ResourceLocation key) {
        var reg = net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(key);
        return reg == null ? null : new ForgeRegistryWrapper((net.minecraft.core.Registry) reg);
    }

    public <V> IForgeRegistry<V> getRegistry(ResourceKey<? extends net.minecraft.core.Registry<V>> key) {
        return getRegistry(key.location());
    }

    public <V> ResourceLocation getName(IForgeRegistry<V> registry) {
        return registry.getRegistryName();
    }
}
