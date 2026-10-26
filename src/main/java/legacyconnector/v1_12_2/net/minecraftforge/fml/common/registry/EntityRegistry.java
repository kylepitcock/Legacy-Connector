package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;

public class EntityRegistry {
    public static void registerModEntity(ResourceLocation registryName, Class<? extends Entity> entityClass, String entityName, int id, Object mod, int trackingRange,
            int updateFrequency, boolean sendsVelocityUpdates) {
        EntityEntry entry = EntityEntryBuilder.create().entity(entityClass).id(registryName, id).name(entityName)
                .tracker(trackingRange, updateFrequency, sendsVelocityUpdates).build();
        ForgeRegistries.ENTITIES.register(entry);
    }

    public static void registerModEntity(ResourceLocation registryName, Class<? extends Entity> entityClass, String entityName, int id, Object mod, int trackingRange,
            int updateFrequency, boolean sendsVelocityUpdates, int eggPrimary, int eggSecondary) {
        EntityEntry entry = EntityEntryBuilder.create().entity(entityClass).id(registryName, id).name(entityName)
                .tracker(trackingRange, updateFrequency, sendsVelocityUpdates).egg(eggPrimary, eggSecondary).build();
        ForgeRegistries.ENTITIES.register(entry);
    }

    public static void registerEgg(ResourceLocation name, int primary, int secondary) {
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(name);
        if (entry == null) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Attempted to register a spawn egg for the unknown entity {}", name);
            return;
        }
        entry.setEgg(new legacyconnector.v1_12_2.net.minecraft.entity.EntityList.EntityEggInfo(name, primary, secondary));
    }

    public static void addSpawn(Class<? extends Entity> entityClass, int weightedProb, int min, int max, EnumCreatureType typeOfCreature, Biome... biomes) {
    }

    public static void removeSpawn(Class<? extends Entity> entityClass, EnumCreatureType typeOfCreature, Biome... biomes) {
    }

    public static EntityEntry getEntry(Class<? extends Entity> entityClass) {
        for (EntityEntry e : ForgeRegistries.ENTITIES.getValues()) {
            if (e.getEntityClass() == entityClass) return e;
        }
        return null;
    }

    public static ResourceLocation getEntityString(Entity entity) {
        EntityEntry e = getEntry(entity.getClass());
        return e == null ? null : e.getRegistryName();
    }
}
