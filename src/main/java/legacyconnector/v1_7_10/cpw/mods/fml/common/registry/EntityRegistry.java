package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts;

/** Legacy entity registration: each registered class becomes a modern entity type run by a host. */
public class EntityRegistry {
    private static final EntityRegistry INSTANCE = new EntityRegistry();
    private static final List<Spawn> SPAWNS = Collections.synchronizedList(new ArrayList<>());
    private static int nextGlobalId = 300;

    /** A natural spawn entry ({@code addSpawn}). */
    public record Spawn(Class<?> entityClass, int weight, int min, int max, EnumCreatureType type, List<BiomeGenBase> biomes) {}

    public static EntityRegistry instance() {
        return INSTANCE;
    }

    private static String activeModId() {
        var c = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        return c == null ? "legacyconnector" : c.record().modernId;
    }

    private static String activeLegacyModId() {
        var c = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
        return c == null ? null : c.record().legacyId;
    }

    public static void registerModEntity(Class<?> entityClass, String entityName, int id, Object mod, int trackingRange,
            int updateFrequency, boolean sendsVelocityUpdates) {
        String legacyMod = activeLegacyModId();
        // FML names mod entities "<legacy mod id>.<name>" (their translation key is entity.<that>.name).
        String legacyName = legacyMod == null ? entityName : legacyMod + "." + entityName;
        LegacyHosts.register(entityClass, entityName, activeModId(), legacyName, trackingRange, updateFrequency, sendsVelocityUpdates);
        // FML recorded mod entities in EntityList's name tables (not its id tables).
        legacyconnector.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.put(legacyName, entityClass);
        legacyconnector.v1_7_10.net.minecraft.entity.EntityList.classToStringMapping.put(entityClass, legacyName);
        legacyconnector.v1_7_10.net.minecraft.entity.EntityList.mcc$registered(entityClass);
        REGISTRATIONS.put(entityClass, new EntityRegistration(entityClass, entityName, id, mod, trackingRange, updateFrequency, sendsVelocityUpdates));
    }

    private static final java.util.Map<Class<?>, EntityRegistration> REGISTRATIONS = new java.util.concurrent.ConcurrentHashMap<>();

    /** What a mod passed to {@link #registerModEntity}. */
    public static class EntityRegistration {
        private final Class<?> entityClass;
        private final String entityName;
        private final int modEntityId;
        private final Object mod;
        private final int trackingRange;
        private final int updateFrequency;
        private final boolean sendsVelocityUpdates;

        EntityRegistration(Class<?> entityClass, String entityName, int id, Object mod, int trackingRange, int updateFrequency, boolean velocity) {
            this.entityClass = entityClass;
            this.entityName = entityName;
            this.modEntityId = id;
            this.mod = mod;
            this.trackingRange = trackingRange;
            this.updateFrequency = updateFrequency;
            this.sendsVelocityUpdates = velocity;
        }

        public Class<?> getEntityClass() {
            return entityClass;
        }

        public String getEntityName() {
            return entityName;
        }

        public int getModEntityId() {
            return modEntityId;
        }

        public Object getContainer() {
            return mod;
        }

        public int getTrackingRange() {
            return trackingRange;
        }

        public int getUpdateFrequency() {
            return updateFrequency;
        }

        public boolean sendsVelocityUpdates() {
            return sendsVelocityUpdates;
        }
    }

    public EntityRegistration lookupModSpawn(Class<?> entityClass, boolean keyByEntityClass) {
        return REGISTRATIONS.get(entityClass);
    }

    /** The registration a mod made with the given mod entity id. */
    public EntityRegistration lookupModSpawn(legacyconnector.v1_7_10.cpw.mods.fml.common.ModContainer mod, int modEntityId) {
        for (EntityRegistration r : REGISTRATIONS.values()) {
            if (r.getModEntityId() == modEntityId && (mod == null || r.getContainer() == null || r.getContainer() == mod
                    || r.getContainer().equals(mod.getMod()))) {
                return r;
            }
        }
        return null;
    }

    public static void registerGlobalEntityID(Class<?> entityClass, String entityName, int id) {
        LegacyHosts.register(entityClass, entityName, activeModId(), 80, 3, true);
        legacyconnector.v1_7_10.net.minecraft.entity.EntityList.addMapping(entityClass, entityName, id);
    }

    public static void registerGlobalEntityID(Class<?> entityClass, String entityName, int id, int backgroundEggColour, int foregroundEggColour) {
        LegacyHosts.register(entityClass, entityName, activeModId(), 80, 3, true);
        // The egg is created by EntityList when the egg info is recorded.
        legacyconnector.v1_7_10.net.minecraft.entity.EntityList.addMapping(entityClass, entityName, id, backgroundEggColour, foregroundEggColour);
    }

    public static synchronized int findGlobalUniqueEntityId() {
        return nextGlobalId++;
    }

    public static void addSpawn(Class<?> entityClass, int weightedProb, int min, int max, EnumCreatureType typeOfCreature, BiomeGenBase... biomes) {
        if (entityClass == null || typeOfCreature == null || biomes == null) return;
        List<BiomeGenBase> list = new ArrayList<>();
        for (BiomeGenBase b : biomes) if (b != null) list.add(b);
        SPAWNS.add(new Spawn(entityClass, weightedProb, min, max, typeOfCreature, list));
    }

    public static void removeSpawn(Class<?> entityClass, EnumCreatureType typeOfCreature, BiomeGenBase... biomes) {
        SPAWNS.removeIf(s -> s.entityClass() == entityClass && s.type() == typeOfCreature);
    }

    public static List<Spawn> mcc$spawns() {
        synchronized (SPAWNS) {
            return List.copyOf(SPAWNS);
        }
    }
}
