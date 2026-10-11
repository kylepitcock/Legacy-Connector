package mccompat.v1_7_10.cpw.mods.fml.common.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.entity.EnumCreatureType;
import mccompat.v1_7_10.net.minecraft.world.biome.BiomeGenBase;
import org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts;

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
        var c = org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710.active();
        return c == null ? "retrofit" : c.record().modernId;
    }

    private static String activeLegacyModId() {
        var c = org.kylepitcock.mccompat.v1_7_10.LegacyModContainer1710.active();
        return c == null ? null : c.record().legacyId;
    }

    public static void registerModEntity(Class<?> entityClass, String entityName, int id, Object mod, int trackingRange,
            int updateFrequency, boolean sendsVelocityUpdates) {
        String legacyMod = activeLegacyModId();
        // FML names mod entities "<legacy mod id>.<name>" (their translation key is entity.<that>.name).
        String legacyName = legacyMod == null ? entityName : legacyMod + "." + entityName;
        LegacyHosts.register(entityClass, entityName, activeModId(), legacyName, trackingRange, updateFrequency, sendsVelocityUpdates);
        // FML recorded mod entities in EntityList's name tables (not its id tables).
        mccompat.v1_7_10.net.minecraft.entity.EntityList.stringToClassMapping.put(legacyName, entityClass);
        mccompat.v1_7_10.net.minecraft.entity.EntityList.classToStringMapping.put(entityClass, legacyName);
    }

    public static void registerGlobalEntityID(Class<?> entityClass, String entityName, int id) {
        LegacyHosts.register(entityClass, entityName, activeModId(), 80, 3, true);
        mccompat.v1_7_10.net.minecraft.entity.EntityList.addMapping(entityClass, entityName, id);
    }

    public static void registerGlobalEntityID(Class<?> entityClass, String entityName, int id, int backgroundEggColour, int foregroundEggColour) {
        LegacyHosts.register(entityClass, entityName, activeModId(), 80, 3, true);
        // The egg is created by EntityList when the egg info is recorded.
        mccompat.v1_7_10.net.minecraft.entity.EntityList.addMapping(entityClass, entityName, id, backgroundEggColour, foregroundEggColour);
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
