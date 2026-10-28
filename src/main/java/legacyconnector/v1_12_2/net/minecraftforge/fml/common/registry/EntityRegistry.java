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

    /**
     * Adds the mob to the biomes' spawn lists, replacing an entry it already has there (as Forge did). The spawn lists become
     * modern biome modifiers when the server's data is generated ({@code Spawns1122}).
     */
    @SuppressWarnings("unchecked")
    public static void addSpawn(Class<? extends Entity> entityClass, int weightedProb, int min, int max, EnumCreatureType typeOfCreature, Biome... biomes) {
        if (entityClass == null || typeOfCreature == null || biomes == null) return;
        for (Biome biome : biomes) {
            if (biome == null) continue;
            var spawns = biome.getSpawnableList(typeOfCreature);
            boolean found = false;
            for (Biome.SpawnListEntry entry : spawns) {
                if (entry.entityClass == entityClass) {
                    entry.itemWeight = weightedProb;
                    entry.minGroupCount = min;
                    entry.maxGroupCount = max;
                    found = true;
                    break;
                }
            }
            if (!found) spawns.add(new Biome.SpawnListEntry(entityClass, weightedProb, min, max));
        }
    }

    /** By the entity's registered name (or legacy name). */
    @SuppressWarnings("unchecked")
    public static void addSpawn(String entityName, int weightedProb, int min, int max, EnumCreatureType spawnList, Biome... biomes) {
        Class<? extends Entity> cls = mcc$classByName(entityName);
        if (cls != null) addSpawn(cls, weightedProb, min, max, spawnList, biomes);
    }

    public static void removeSpawn(Class<? extends Entity> entityClass, EnumCreatureType typeOfCreature, Biome... biomes) {
        if (entityClass == null || typeOfCreature == null || biomes == null) return;
        for (Biome biome : biomes) {
            if (biome != null) biome.getSpawnableList(typeOfCreature).removeIf(entry -> entry.entityClass == entityClass);
        }
    }

    public static void removeSpawn(String entityName, EnumCreatureType spawnList, Biome... biomes) {
        Class<? extends Entity> cls = mcc$classByName(entityName);
        if (cls != null) removeSpawn(cls, spawnList, biomes);
    }

    private static Class<? extends Entity> mcc$classByName(String name) {
        if (name == null) return null;
        for (EntityEntry e : ForgeRegistries.ENTITIES.getValues()) {
            if (name.equals(e.getName()) || e.getRegistryName() != null && name.equals(e.getRegistryName().toString())) return e.getEntityClass();
        }
        return null;
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

    // ---- area F: FML's registration records

    private static final EntityRegistry INSTANCE = new EntityRegistry();

    public static EntityRegistry instance() {
        return INSTANCE;
    }

    /** The registration of a mod entity class (its registry entry); null when none. */
    public EntityRegistration lookupModSpawn(Class<? extends Entity> clazz, boolean keepLooking) {
        for (Class<?> c = clazz; c != null && Entity.class.isAssignableFrom(c); c = keepLooking ? c.getSuperclass() : null) {
            for (EntityEntry e : ForgeRegistries.ENTITIES.getValues()) {
                if (e.getEntityClass() == c) return new EntityRegistration(e);
            }
        }
        return null;
    }

    /** The registration a mod made under its own network id. */
    public EntityRegistration lookupModSpawn(legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer mc, int modEntityId) {
        String modId = mc == null ? null : mc.getModId();
        for (EntityEntry e : ForgeRegistries.ENTITIES.getValues()) {
            if (e.mcc$networkId == modEntityId && e.getRegistryName() != null && (modId == null || modId.equals(e.getRegistryName().getNamespace()))) {
                return new EntityRegistration(e);
            }
        }
        return null;
    }

    public static class EntityRegistration {
        private final EntityEntry entry;

        EntityRegistration(EntityEntry entry) {
            this.entry = entry;
        }

        public ResourceLocation getRegistryName() {
            return entry.getRegistryName();
        }

        public Class<? extends Entity> getEntityClass() {
            return entry.getEntityClass();
        }

        public legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer getContainer() {
            var name = entry.getRegistryName();
            return name == null ? null : legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(name.getNamespace());
        }

        public String getEntityName() {
            return entry.getName();
        }

        public int getModEntityId() {
            return entry.mcc$networkId;
        }

        public int getTrackingRange() {
            return entry.trackingRange;
        }

        public int getUpdateFrequency() {
            return entry.updateFrequency;
        }

        public boolean sendsVelocityUpdates() {
            return entry.sendsVelocityUpdates;
        }

        public boolean usesVanillaSpawning() {
            return false;
        }
    }
}
