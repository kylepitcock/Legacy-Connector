package legacyconnector.v1_6_4.facade.cpw.mods.fml.common.registry;

/** Facade of the 1.7.10 shim's {@code EntityRegistry} for 1.5.2 and 1.6.4 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class EntityRegistry {
    private EntityRegistry() {}

    /**
     * {@code addSpawn(entityName, weight, min, max, type, biomes...)}: the spawn of the entity registered under that name ({@code EntityList} string id,
     * which {@code registerGlobalEntityID} and {@code registerModEntity} fill); an unknown name adds nothing, as Forge looked the class up the same way.
     */
    public static void addSpawn(String name, int weight, int min, int max, legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type,
            legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase... biomes) {
        Class<?> cls = legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(name);
        if (cls != null) legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry.addSpawn(cls, weight, min, max, type, biomes);
    }
}
