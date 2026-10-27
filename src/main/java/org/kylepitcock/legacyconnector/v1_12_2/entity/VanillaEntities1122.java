package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The vanilla 1.12.2 entity classes and the modern types they are.
 *
 * <ul>
 *     <li>Exact vanilla classes mods construct themselves ({@code new EntityItem(world, x, y, z, stack)}, {@code new EntityZombie(world)})
 *     become views of a new modern entity of that type, so the game runs, renders and saves it as vanilla does. A mod's subclass is
 *     a legacy entity with a host as usual.</li>
 *     <li>Modern entities get a view of their 1.12 class ({@code instanceof EntityZombie}, {@code getEntitiesWithinAABB(EntityMob.class)}):
 *     the vanilla class (hand-written or synthesized) when the type is a vanilla one, otherwise the closest legacy base class.</li>
 * </ul>
 */
public final class VanillaEntities1122 {
    private VanillaEntities1122() {}

    private static final String SHIM = "legacyconnector.v1_12_2.net.minecraft.entity.";
    private static final String SYNTH = "legacyconnector_synth.v1_12_2.net.minecraft.entity.";

    /** Legacy class (without the package prefix) -> modern type. */
    private static final Map<String, Supplier<EntityType<?>>> TYPES = new HashMap<>();
    /** Modern type -> legacy class name (filled lazily from TYPES). */
    private static volatile Map<EntityType<?>, String> BY_TYPE;

    private static void put(String legacy, Supplier<EntityType<?>> type) {
        TYPES.put(legacy, type);
    }

    static {
        put("item.EntityItem", () -> EntityType.ITEM);
        put("item.EntityXPOrb", () -> EntityType.EXPERIENCE_ORB);
        put("effect.EntityLightningBolt", () -> EntityType.LIGHTNING_BOLT);
        put("item.EntityTNTPrimed", () -> EntityType.TNT);
        put("item.EntityFallingBlock", () -> EntityType.FALLING_BLOCK);
        put("item.EntityBoat", () -> EntityType.BOAT);
        put("item.EntityArmorStand", () -> EntityType.ARMOR_STAND);
        put("item.EntityItemFrame", () -> EntityType.ITEM_FRAME);
        put("item.EntityPainting", () -> EntityType.PAINTING);
        put("item.EntityEnderCrystal", () -> EntityType.END_CRYSTAL);
        put("item.EntityEnderPearl", () -> EntityType.ENDER_PEARL);
        put("item.EntityEnderEye", () -> EntityType.EYE_OF_ENDER);
        put("item.EntityExpBottle", () -> EntityType.EXPERIENCE_BOTTLE);
        put("item.EntityFireworkRocket", () -> EntityType.FIREWORK_ROCKET);
        put("item.EntityMinecartEmpty", () -> EntityType.MINECART);
        put("item.EntityMinecartChest", () -> EntityType.CHEST_MINECART);
        put("item.EntityMinecartFurnace", () -> EntityType.FURNACE_MINECART);
        put("item.EntityMinecartTNT", () -> EntityType.TNT_MINECART);
        put("item.EntityMinecartHopper", () -> EntityType.HOPPER_MINECART);
        put("item.EntityMinecartMobSpawner", () -> EntityType.SPAWNER_MINECART);
        put("item.EntityMinecartCommandBlock", () -> EntityType.COMMAND_BLOCK_MINECART);
        put("EntityLeashKnot", () -> EntityType.LEASH_KNOT);
        put("EntityAreaEffectCloud", () -> EntityType.AREA_EFFECT_CLOUD);
        put("projectile.EntityTippedArrow", () -> EntityType.ARROW);
        put("projectile.EntitySpectralArrow", () -> EntityType.SPECTRAL_ARROW);
        put("projectile.EntitySnowball", () -> EntityType.SNOWBALL);
        put("projectile.EntityEgg", () -> EntityType.EGG);
        put("projectile.EntityPotion", () -> EntityType.POTION);
        put("projectile.EntityLargeFireball", () -> EntityType.FIREBALL);
        put("projectile.EntitySmallFireball", () -> EntityType.SMALL_FIREBALL);
        put("projectile.EntityDragonFireball", () -> EntityType.DRAGON_FIREBALL);
        put("projectile.EntityWitherSkull", () -> EntityType.WITHER_SKULL);
        put("projectile.EntityShulkerBullet", () -> EntityType.SHULKER_BULLET);
        put("projectile.EntityLlamaSpit", () -> EntityType.LLAMA_SPIT);
        put("projectile.EntityEvokerFangs", () -> EntityType.EVOKER_FANGS);
        put("projectile.EntityFishHook", () -> EntityType.FISHING_BOBBER);
        put("monster.EntityCreeper", () -> EntityType.CREEPER);
        put("monster.EntitySkeleton", () -> EntityType.SKELETON);
        put("monster.EntityStray", () -> EntityType.STRAY);
        put("monster.EntityWitherSkeleton", () -> EntityType.WITHER_SKELETON);
        put("monster.EntitySpider", () -> EntityType.SPIDER);
        put("monster.EntityCaveSpider", () -> EntityType.CAVE_SPIDER);
        put("monster.EntityGiantZombie", () -> EntityType.GIANT);
        put("monster.EntityZombie", () -> EntityType.ZOMBIE);
        put("monster.EntityHusk", () -> EntityType.HUSK);
        put("monster.EntityZombieVillager", () -> EntityType.ZOMBIE_VILLAGER);
        put("monster.EntityPigZombie", () -> EntityType.ZOMBIFIED_PIGLIN);
        put("monster.EntitySlime", () -> EntityType.SLIME);
        put("monster.EntityMagmaCube", () -> EntityType.MAGMA_CUBE);
        put("monster.EntityGhast", () -> EntityType.GHAST);
        put("monster.EntityEnderman", () -> EntityType.ENDERMAN);
        put("monster.EntityEndermite", () -> EntityType.ENDERMITE);
        put("monster.EntitySilverfish", () -> EntityType.SILVERFISH);
        put("monster.EntityBlaze", () -> EntityType.BLAZE);
        put("monster.EntityWitch", () -> EntityType.WITCH);
        put("monster.EntityGuardian", () -> EntityType.GUARDIAN);
        put("monster.EntityElderGuardian", () -> EntityType.ELDER_GUARDIAN);
        put("monster.EntityShulker", () -> EntityType.SHULKER);
        put("monster.EntityVex", () -> EntityType.VEX);
        put("monster.EntityEvoker", () -> EntityType.EVOKER);
        put("monster.EntityVindicator", () -> EntityType.VINDICATOR);
        put("monster.EntityIllusionIllager", () -> EntityType.ILLUSIONER);
        put("monster.EntitySnowman", () -> EntityType.SNOW_GOLEM);
        put("monster.EntityIronGolem", () -> EntityType.IRON_GOLEM);
        put("monster.EntityPolarBear", () -> EntityType.POLAR_BEAR);
        put("boss.EntityWither", () -> EntityType.WITHER);
        put("boss.EntityDragon", () -> EntityType.ENDER_DRAGON);
        put("passive.EntityBat", () -> EntityType.BAT);
        put("passive.EntityPig", () -> EntityType.PIG);
        put("passive.EntitySheep", () -> EntityType.SHEEP);
        put("passive.EntityCow", () -> EntityType.COW);
        put("passive.EntityMooshroom", () -> EntityType.MOOSHROOM);
        put("passive.EntityChicken", () -> EntityType.CHICKEN);
        put("passive.EntitySquid", () -> EntityType.SQUID);
        put("passive.EntityWolf", () -> EntityType.WOLF);
        put("passive.EntityOcelot", () -> EntityType.OCELOT);
        put("passive.EntityRabbit", () -> EntityType.RABBIT);
        put("passive.EntityParrot", () -> EntityType.PARROT);
        put("passive.EntityHorse", () -> EntityType.HORSE);
        put("passive.EntityDonkey", () -> EntityType.DONKEY);
        put("passive.EntityMule", () -> EntityType.MULE);
        put("passive.EntitySkeletonHorse", () -> EntityType.SKELETON_HORSE);
        put("passive.EntityZombieHorse", () -> EntityType.ZOMBIE_HORSE);
        put("passive.EntityLlama", () -> EntityType.LLAMA);
        put("passive.EntityVillager", () -> EntityType.VILLAGER);
    }

    private static String legacyName(Class<?> cls) {
        String n = cls.getName();
        if (n.startsWith(SHIM)) return n.substring(SHIM.length());
        if (n.startsWith(SYNTH)) return n.substring(SYNTH.length());
        return null;
    }

    /** Whether instances of exactly this class are views of a modern vanilla entity. */
    public static boolean isVanilla(Class<?> cls) {
        String n = legacyName(cls);
        return n != null && TYPES.containsKey(n);
    }

    /**
     * Called by the legacy {@code Entity(World)} constructor: the modern entity this object is a view of, or null when it is a mod
     * entity (any class that is not exactly a vanilla one, or no real level).
     */
    public static net.minecraft.world.entity.Entity create(Entity legacy, World world) {
        String n = legacyName(legacy.getClass());
        Supplier<EntityType<?>> type = n == null ? null : TYPES.get(n);
        if (type == null) return null;
        Level level = world == null ? null : world.mcc$level();
        if (level == null) return null;
        try {
            return type.get().create(level);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ views of modern entities

    private static Map<EntityType<?>, String> byType() {
        var m = BY_TYPE;
        if (m == null) {
            Map<EntityType<?>, String> made = new IdentityHashMap<>();
            TYPES.forEach((legacy, type) -> {
                try {
                    made.putIfAbsent(type.get(), legacy);
                } catch (RuntimeException | LinkageError e) {
                    // a type this game version lacks
                }
            });
            BY_TYPE = m = made;
        }
        return m;
    }

    private static final Map<String, Class<?>> CLASSES = new ConcurrentHashMap<>();
    private static final Class<?> MISSING = Void.class;

    /** The legacy class (hand-written or synthesized) of a legacy name, or null (cached either way). */
    public static Class<?> mcc$legacyClass(String name) {
        Class<?> c = CLASSES.computeIfAbsent(name, n -> {
            for (String prefix : new String[] {SHIM, SYNTH}) {
                for (ClassLoader loader : new ClassLoader[] {Entity.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
                    try {
                        Class<?> found = Class.forName(prefix + n, false, loader);
                        if (Entity.class.isAssignableFrom(found) && !java.lang.reflect.Modifier.isAbstract(found.getModifiers())) return found;
                    } catch (ClassNotFoundException | LinkageError e) {
                        // next
                    }
                }
            }
            return MISSING;
        });
        return c == MISSING ? null : c;
    }

    /** The vanilla legacy class name of a modern type, or null. */
    public static String mcc$legacyNameOf(EntityType<?> type) {
        return byType().get(type);
    }
}
