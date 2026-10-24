package legacyconnector.v1_7_10.net.minecraft.tileentity;

import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts;

/** The spawn settings of a vanilla spawner block: the entity name legacy code sets maps to the modern spawner's entity type. */
public class MobSpawnerBaseLogic {
    private static final Map<String, String> VANILLA = Map.ofEntries(
            Map.entry("Zombie", "zombie"), Map.entry("Skeleton", "skeleton"), Map.entry("Spider", "spider"), Map.entry("CaveSpider", "cave_spider"),
            Map.entry("Creeper", "creeper"), Map.entry("Blaze", "blaze"), Map.entry("Silverfish", "silverfish"), Map.entry("Enderman", "enderman"),
            Map.entry("Slime", "slime"), Map.entry("PigZombie", "zombified_piglin"), Map.entry("Ghast", "ghast"), Map.entry("LavaSlime", "magma_cube"),
            Map.entry("Witch", "witch"), Map.entry("Villager", "villager"), Map.entry("Pig", "pig"), Map.entry("Cow", "cow"),
            Map.entry("Chicken", "chicken"), Map.entry("Sheep", "sheep"), Map.entry("Wolf", "wolf"), Map.entry("Bat", "bat"),
            Map.entry("Squid", "squid"), Map.entry("Ozelot", "ocelot"), Map.entry("EntityHorse", "horse"), Map.entry("Giant", "giant"),
            Map.entry("MushroomCow", "mooshroom"), Map.entry("VillagerGolem", "iron_golem"), Map.entry("SnowMan", "snow_golem"),
            Map.entry("WitherBoss", "wither"), Map.entry("EnderDragon", "ender_dragon"));

    private final SpawnerBlockEntity host;
    private String entityName = "Pig";

    public MobSpawnerBaseLogic(SpawnerBlockEntity host) {
        this.host = host;
    }

    public void setEntityName(String name) {
        this.entityName = name == null ? "Pig" : name;
        EntityType<?> type = typeFor(entityName);
        if (type != null && host != null) host.setEntityId(type, RandomSource.create());
    }

    public String getEntityNameToSpawn() {
        return entityName;
    }

    private static EntityType<?> typeFor(String name) {
        String vanilla = VANILLA.get(name);
        if (vanilla != null) return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace(vanilla));
        Class<?> cls = EntityList.stringToClassMapping.get(name);
        if (cls != null) {
            var info = LegacyHosts.info(cls);
            if (info != null) return info.type();
        }
        return null;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.World getSpawnerWorld() {
        return null;
    }

    public int getSpawnerX() {
        return 0;
    }

    public int getSpawnerY() {
        return 0;
    }

    public int getSpawnerZ() {
        return 0;
    }
}
