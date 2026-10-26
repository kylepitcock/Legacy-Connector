package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Vanilla 1.7.10 entity classes that mods construct themselves ({@code new EntityGhast(world)}, {@code new EntitySnowball(world, x, y, z)},
 * {@code EntityList.createEntityByName("Zombie", world)}) become views of the real modern entity, so the game runs, renders and
 * saves them as vanilla does. Only the exact vanilla class: a mod's subclass is a legacy entity with a host as usual.
 */
public final class VanillaEntities {
    private VanillaEntities() {}

    /** Legacy class (without the shim's package prefix) -> modern type. */
    private static final Map<String, Supplier<EntityType<?>>> TYPES = new HashMap<>();

    static {
        put("monster.EntityCreeper", () -> EntityType.CREEPER);
        put("monster.EntitySkeleton", () -> EntityType.SKELETON);
        put("monster.EntitySpider", () -> EntityType.SPIDER);
        put("monster.EntityGiantZombie", () -> EntityType.GIANT);
        put("monster.EntityZombie", () -> EntityType.ZOMBIE);
        put("monster.EntitySlime", () -> EntityType.SLIME);
        put("monster.EntityGhast", () -> EntityType.GHAST);
        put("monster.EntityPigZombie", () -> EntityType.ZOMBIFIED_PIGLIN);
        put("monster.EntityEnderman", () -> EntityType.ENDERMAN);
        put("monster.EntityCaveSpider", () -> EntityType.CAVE_SPIDER);
        put("monster.EntitySilverfish", () -> EntityType.SILVERFISH);
        put("monster.EntityBlaze", () -> EntityType.BLAZE);
        put("monster.EntityMagmaCube", () -> EntityType.MAGMA_CUBE);
        put("monster.EntityWitch", () -> EntityType.WITCH);
        put("monster.EntitySnowman", () -> EntityType.SNOW_GOLEM);
        put("monster.EntityIronGolem", () -> EntityType.IRON_GOLEM);
        put("boss.EntityWither", () -> EntityType.WITHER);
        put("passive.EntityBat", () -> EntityType.BAT);
        put("passive.EntityPig", () -> EntityType.PIG);
        put("passive.EntitySheep", () -> EntityType.SHEEP);
        put("passive.EntityCow", () -> EntityType.COW);
        put("passive.EntityChicken", () -> EntityType.CHICKEN);
        put("passive.EntitySquid", () -> EntityType.SQUID);
        put("passive.EntityWolf", () -> EntityType.WOLF);
        put("passive.EntityMooshroom", () -> EntityType.MOOSHROOM);
        put("passive.EntityOcelot", () -> EntityType.OCELOT);
        put("passive.EntityHorse", () -> EntityType.HORSE);
        put("passive.EntityVillager", () -> EntityType.VILLAGER);
        put("projectile.EntitySnowball", () -> EntityType.SNOWBALL);
        put("projectile.EntityEgg", () -> EntityType.EGG);
        put("item.EntityEnderPearl", () -> EntityType.ENDER_PEARL);
        put("item.EntityExpBottle", () -> EntityType.EXPERIENCE_BOTTLE);
        put("item.EntityEnderCrystal", () -> EntityType.END_CRYSTAL);
        put("EntityLeashKnot", () -> EntityType.LEASH_KNOT);
        put("item.EntityItemFrame", () -> EntityType.ITEM_FRAME);
        put("item.EntityPainting", () -> EntityType.PAINTING);
        put("item.EntityMinecartEmpty", () -> EntityType.MINECART);
        put("item.EntityMinecartChest", () -> EntityType.CHEST_MINECART);
        put("item.EntityMinecartFurnace", () -> EntityType.FURNACE_MINECART);
        put("item.EntityMinecartTNT", () -> EntityType.TNT_MINECART);
        put("item.EntityMinecartHopper", () -> EntityType.HOPPER_MINECART);
        put("ai.EntityMinecartMobSpawner", () -> EntityType.SPAWNER_MINECART);
        put("EntityMinecartCommandBlock", () -> EntityType.COMMAND_BLOCK_MINECART);
    }

    private static void put(String legacy, Supplier<EntityType<?>> type) {
        TYPES.put(legacy, type);
    }

    private static String legacyName(Class<?> cls) {
        String n = cls.getName();
        for (String prefix : new String[] {"legacyconnector.v1_7_10.net.minecraft.entity.", "legacyconnector_synth.v1_7_10.net.minecraft.entity."}) {
            if (n.startsWith(prefix)) return n.substring(prefix.length());
        }
        return null;
    }

    /** Whether instances of exactly this class are views of a modern vanilla entity. */
    public static boolean isVanilla(Class<?> cls) {
        String n = legacyName(cls);
        return n != null && TYPES.containsKey(n);
    }

    /**
     * Called by the legacy {@code Entity(World)} constructor: the modern entity this object is a view of, or null when it is a
     * legacy entity (any class that is not exactly a vanilla one, or no real level).
     */
    public static net.minecraft.world.entity.Entity create(Entity legacy, World world) {
        String n = legacyName(legacy.getClass());
        Supplier<EntityType<?>> type = n == null ? null : TYPES.get(n);
        if (type == null) return null;
        Level level = world == null ? null : world.mcc$fullLevel();
        if (level == null) return null;
        try {
            return type.get().create(level);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
