package legacyconnector.v1_12_2.net.minecraftforge.registries;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Forge's registries by name ({@code minecraft:blocks}, mods' own) and by entry type. Only the active set exists here. */
public class RegistryManager {
    public static final RegistryManager ACTIVE = new RegistryManager("ACTIVE");
    public static final RegistryManager VANILLA = ACTIVE;
    public static final RegistryManager FROZEN = ACTIVE;

    private final String name;

    public RegistryManager(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static Map<ResourceLocation, IForgeRegistry<?>> vanilla() {
        Map<ResourceLocation, IForgeRegistry<?>> out = new LinkedHashMap<>();
        out.put(new ResourceLocation("blocks"), ForgeRegistries.BLOCKS);
        out.put(new ResourceLocation("items"), ForgeRegistries.ITEMS);
        out.put(new ResourceLocation("potions"), ForgeRegistries.POTIONS);
        out.put(new ResourceLocation("biomes"), ForgeRegistries.BIOMES);
        out.put(new ResourceLocation("soundevents"), ForgeRegistries.SOUND_EVENTS);
        out.put(new ResourceLocation("potiontypes"), ForgeRegistries.POTION_TYPES);
        out.put(new ResourceLocation("enchantments"), ForgeRegistries.ENCHANTMENTS);
        out.put(new ResourceLocation("villagerprofessions"), ForgeRegistries.VILLAGER_PROFESSIONS);
        out.put(new ResourceLocation("entities"), ForgeRegistries.ENTITIES);
        out.put(new ResourceLocation("recipes"), ForgeRegistries.RECIPES);
        return out;
    }

    @SuppressWarnings("unchecked")
    public <V extends IForgeRegistryEntry<V>> ForgeRegistry<V> getRegistry(ResourceLocation key) {
        if (key == null) return null;
        IForgeRegistry<?> reg = vanilla().get(key);
        if (reg == null) {
            for (var custom : org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.customRegistries()) {
                if (key.equals(org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.customName(custom))) reg = custom;
            }
        }
        return reg instanceof ForgeRegistry<?> f ? (ForgeRegistry<V>) f : null;
    }

    public <V extends IForgeRegistryEntry<V>> IForgeRegistry<V> getRegistry(Class<? super V> cls) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        IForgeRegistry<V> reg = (IForgeRegistry<V>) legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.GameRegistry.findRegistry((Class) cls);
        return reg;
    }

    public <V extends IForgeRegistryEntry<V>> ResourceLocation getName(IForgeRegistry<V> reg) {
        for (var e : vanilla().entrySet()) if (e.getValue() == reg) return e.getKey();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.customName(reg);
    }

    /** The registries' contents by name (what Forge saved into the world); ids are the modern game's business here. */
    public Map<ResourceLocation, Object> takeSnapshot(boolean savingToDisc) {
        Map<ResourceLocation, Object> out = new LinkedHashMap<>();
        for (var e : vanilla().entrySet()) out.put(e.getKey(), e.getValue().getKeys());
        return out;
    }
}
