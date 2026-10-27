package legacyconnector.v1_12_2.net.minecraftforge.registries;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/**
 * Forge's registry manager: the registries by name. There is one set of legacy registries, so ACTIVE, VANILLA and FROZEN are the same
 * view. The vanilla registries have their 1.12 names ({@code minecraft:blocks}, ...); registries mods made keep the name they gave.
 * Libraries name registries through {@link #getName} (SuperMartijn642's Core Lib keys its wrappers by it).
 */
public class RegistryManager {
    public static final RegistryManager ACTIVE = new RegistryManager();
    public static final RegistryManager VANILLA = ACTIVE;
    public static final RegistryManager FROZEN = ACTIVE;

    private static Map<ResourceLocation, IForgeRegistry<?>> byName() {
        Map<ResourceLocation, IForgeRegistry<?>> out = new LinkedHashMap<>();
        out.put(new ResourceLocation("minecraft", "blocks"), Registries1122.BLOCKS);
        out.put(new ResourceLocation("minecraft", "items"), Registries1122.ITEMS);
        out.put(new ResourceLocation("minecraft", "potions"), Registries1122.POTIONS);
        out.put(new ResourceLocation("minecraft", "biomes"), Registries1122.BIOMES);
        out.put(new ResourceLocation("minecraft", "soundevents"), Registries1122.SOUND_EVENTS);
        out.put(new ResourceLocation("minecraft", "potiontypes"), Registries1122.POTION_TYPES);
        out.put(new ResourceLocation("minecraft", "enchantments"), Registries1122.ENCHANTMENTS);
        out.put(new ResourceLocation("minecraft", "villagerprofessions"), Registries1122.VILLAGER_PROFESSIONS);
        out.put(new ResourceLocation("minecraft", "entities"), Registries1122.ENTITIES);
        out.put(new ResourceLocation("minecraft", "recipes"), Registries1122.RECIPES);
        out.putAll(Registries1122.customRegistryNames());
        return out;
    }

    @SuppressWarnings("unchecked")
    public <V extends IForgeRegistryEntry<V>> ForgeRegistry<V> getRegistry(ResourceLocation key) {
        return (ForgeRegistry<V>) byName().get(key);
    }

    @SuppressWarnings("unchecked")
    public <V extends IForgeRegistryEntry<V>> ForgeRegistry<V> getRegistry(Class<? super V> type) {
        return (ForgeRegistry<V>) legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.findRegistry((Class) type);
    }

    public <V extends IForgeRegistryEntry<V>> ResourceLocation getName(IForgeRegistry<V> registry) {
        Map<IForgeRegistry<?>, ResourceLocation> names = new IdentityHashMap<>();
        for (var e : byName().entrySet()) names.putIfAbsent(e.getValue(), e.getKey());
        return names.get(registry);
    }
}
