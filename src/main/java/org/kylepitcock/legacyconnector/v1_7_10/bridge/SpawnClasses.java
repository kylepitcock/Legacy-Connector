package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts;

/**
 * Legacy entity classes (as spawn lists name them) to modern entity types and back: a mod's entities through their
 * registration, vanilla's through their 1.7.10 names ({@code EntityList}) and the modern ids those became.
 */
final class SpawnClasses {
    private SpawnClasses() {}

    /** 1.7.10 entity names whose modern id is not the name in snake case. */
    private static final Map<String, String> RENAMED = Map.ofEntries(Map.entry("PigZombie", "zombified_piglin"), Map.entry("LavaSlime", "magma_cube"),
            Map.entry("EnderDragon", "ender_dragon"), Map.entry("WitherBoss", "wither"), Map.entry("MushroomCow", "mooshroom"),
            Map.entry("SnowMan", "snow_golem"), Map.entry("Ozelot", "ocelot"), Map.entry("VillagerGolem", "iron_golem"),
            Map.entry("EntityHorse", "horse"), Map.entry("CaveSpider", "cave_spider"), Map.entry("Giant", "giant"));

    private static String snake(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    /** The modern type of a legacy entity class, or null. */
    static EntityType<?> modernType(Class<?> cls) {
        if (cls == null) return null;
        LegacyHosts.Info info = LegacyHosts.info(cls);
        if (info != null) return info.type();
        String name = EntityList.classToStringMapping.get(cls);
        if (name == null) return null;
        String id = RENAMED.getOrDefault(name, snake(name));
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.withDefaultNamespace(id)).orElse(null);
    }

    /** The legacy class of a modern entity type: the mod's class, vanilla's 1.7.10 class, or the closest legacy base. */
    static Class<?> legacyClass(EntityType<?> type) {
        return CLASSES.computeIfAbsent(type, SpawnClasses::findLegacyClass);
    }

    private static final Map<EntityType<?>, Class<?>> CLASSES = new java.util.concurrent.ConcurrentHashMap<>();

    private static Class<?> findLegacyClass(EntityType<?> type) {
        LegacyHosts.Info info = LegacyHosts.info(type);
        if (info != null) return info.legacyClass();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id.getNamespace().equals("minecraft")) {
            for (Map.Entry<Class<?>, String> e : EntityList.classToStringMapping.entrySet()) {
                if (RENAMED.getOrDefault(e.getValue(), snake(e.getValue())).equals(id.getPath())) return e.getKey();
            }
        }
        return switch (type.getCategory()) {
            case MONSTER -> legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityMob.class;
            case CREATURE -> legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal.class;
            case AMBIENT -> legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAmbientCreature.class;
            default -> legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving.class;
        };
    }
}
