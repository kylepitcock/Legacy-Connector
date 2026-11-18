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

    /** {@code removeSpawn(entityName, type, biomes...)}: takes the spawn of the entity registered under that name away again; an unknown name removes nothing. */
    public static void removeSpawn(String name, legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type,
            legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase... biomes) {
        Class<?> cls = legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.get(name);
        if (cls != null) legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry.removeSpawn(cls, type, biomes);
    }

    /**
     * 1.5.2 {@code registerModLoaderEntity(mod, class, id, trackingRange, updateFrequency, sendsVelocity)}: a ModLoader mod's entity (its name came from
     * {@code ModLoader.registerEntityID}, which put the class into the entity name table) registered with the tracking values of the mod.
     */
    public static legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry.EntityRegistration registerModLoaderEntity(Object mod, Class<?> entityClass, int id,
            int trackingRange, int updateFrequency, boolean sendsVelocityUpdates) {
        Object named = legacyconnector.v1_7_10.net.minecraft.entity.EntityList.classToStringMapping.get(entityClass);
        String name = named instanceof String s ? s : entityClass.getSimpleName();
        legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry.registerModEntity(entityClass, name, id, mod, trackingRange, updateFrequency, sendsVelocityUpdates);
        return legacyconnector.v1_7_10.cpw.mods.fml.common.registry.EntityRegistry.instance().lookupModSpawn(entityClass, true);
    }
}
