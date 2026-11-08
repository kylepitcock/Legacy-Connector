package legacyconnector.v1_12_2.net.minecraft.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.SpawnPlacementType;

/** 1.12.2 table of where each creature class spawns: on the ground unless registered otherwise (squids in water, bats and ghasts in the air). */
public final class EntitySpawnPlacementRegistry {
    private static final Map<Class<?>, SpawnPlacementType> ENTITY_PLACEMENTS = new ConcurrentHashMap<>();

    static {
        ENTITY_PLACEMENTS.put(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntitySquid.class, SpawnPlacementType.IN_WATER);
        ENTITY_PLACEMENTS.put(legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityBat.class, SpawnPlacementType.IN_AIR);
        ENTITY_PLACEMENTS.put(legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityGhast.class, SpawnPlacementType.IN_AIR);
    }

    private EntitySpawnPlacementRegistry() {}

    /** Where the creature class spawns (the nearest registered superclass counts; on the ground by default). */
    public static SpawnPlacementType getPlacementForEntity(Class<?> entityClass) {
        for (Class<?> c = entityClass; c != null; c = c.getSuperclass()) {
            SpawnPlacementType t = ENTITY_PLACEMENTS.get(c);
            if (t != null) return t;
        }
        return SpawnPlacementType.ON_GROUND;
    }

    public static void setPlacementType(Class<?> entityClass, SpawnPlacementType placementType) {
        if (entityClass != null && placementType != null) ENTITY_PLACEMENTS.put(entityClass, placementType);
    }
}
