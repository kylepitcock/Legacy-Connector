package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacySpawnEggs;

/**
 * 1.7.10's entity id tables. Mods register through FML ({@code EntityRegistry}) or write these maps directly; an
 * {@link EntityEggInfo} put in {@link #entityEggs} for an id whose class is known becomes a modern spawn egg (in
 * either order), which is how mods such as Biomes O' Plenty gave their mobs eggs.
 */
public class EntityList {
    public static Map<String, Class<?>> stringToClassMapping = new HashMap<>();
    public static Map<Class<?>, String> classToStringMapping = new HashMap<>();
    public static Map<Integer, Class<?>> IDtoClassMapping = new HashMap<>() {
        @Override
        public Class<?> put(Integer id, Class<?> cls) {
            Class<?> old = super.put(id, cls);
            if (id != null && cls != null && entityEggs != null && entityEggs.get(id) instanceof EntityEggInfo egg) mcc$egg(cls, egg);
            return old;
        }
    };
    public static Map<Class<?>, Integer> classToIDMapping = new HashMap<>();
    public static Map<String, Integer> stringToIDMapping = new HashMap<>();

    /** Vanilla's 1.7.10 entity classes, which have no behaviour here (they are not created through these maps). */
    private static final java.util.Set<Class<?>> VANILLA = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Name, id and class (relative to net.minecraft.entity) of every entity vanilla 1.7.10 registered. */
    private static final String[][] VANILLA_ENTITIES = {
            {"Item", "1", "item.EntityItem"}, {"XPOrb", "2", "item.EntityXPOrb"}, {"LeashKnot", "8", "EntityLeashKnot"},
            {"Painting", "9", "item.EntityPainting"}, {"Arrow", "10", "projectile.EntityArrow"}, {"Snowball", "11", "projectile.EntitySnowball"},
            {"Fireball", "12", "projectile.EntityLargeFireball"}, {"SmallFireball", "13", "projectile.EntitySmallFireball"},
            {"ThrownEnderpearl", "14", "projectile.EntityEnderPearl"}, {"EyeOfEnderSignal", "15", "item.EntityEnderEye"},
            {"ThrownPotion", "16", "projectile.EntityPotion"}, {"ThrownExpBottle", "17", "item.EntityExpBottle"},
            {"ItemFrame", "18", "item.EntityItemFrame"}, {"WitherSkull", "19", "projectile.EntityWitherSkull"},
            {"PrimedTnt", "20", "item.EntityTNTPrimed"}, {"FallingSand", "21", "item.EntityFallingBlock"},
            {"FireworksRocketEntity", "22", "item.EntityFireworkRocket"}, {"Boat", "41", "item.EntityBoat"},
            {"MinecartRideable", "42", "item.EntityMinecartEmpty"}, {"MinecartChest", "43", "item.EntityMinecartChest"},
            {"MinecartFurnace", "44", "item.EntityMinecartFurnace"}, {"MinecartTNT", "45", "item.EntityMinecartTNT"},
            {"MinecartHopper", "46", "item.EntityMinecartHopper"}, {"MinecartSpawner", "47", "item.EntityMinecartMobSpawner"},
            {"MinecartCommandBlock", "40", "item.EntityMinecartCommandBlock"},
            {"Creeper", "50", "monster.EntityCreeper"}, {"Skeleton", "51", "monster.EntitySkeleton"}, {"Spider", "52", "monster.EntitySpider"},
            {"Giant", "53", "monster.EntityGiantZombie"}, {"Zombie", "54", "monster.EntityZombie"}, {"Slime", "55", "monster.EntitySlime"},
            {"Ghast", "56", "monster.EntityGhast"}, {"PigZombie", "57", "monster.EntityPigZombie"}, {"Enderman", "58", "monster.EntityEnderman"},
            {"CaveSpider", "59", "monster.EntityCaveSpider"}, {"Silverfish", "60", "monster.EntitySilverfish"}, {"Blaze", "61", "monster.EntityBlaze"},
            {"LavaSlime", "62", "monster.EntityMagmaCube"}, {"EnderDragon", "63", "boss.EntityDragon"}, {"WitherBoss", "64", "boss.EntityWither"},
            {"Bat", "65", "passive.EntityBat"}, {"Witch", "66", "monster.EntityWitch"}, {"Pig", "90", "passive.EntityPig"},
            {"Sheep", "91", "passive.EntitySheep"}, {"Cow", "92", "passive.EntityCow"}, {"Chicken", "93", "passive.EntityChicken"},
            {"Squid", "94", "passive.EntitySquid"}, {"Wolf", "95", "passive.EntityWolf"}, {"MushroomCow", "96", "passive.EntityMooshroom"},
            {"SnowMan", "97", "monster.EntitySnowman"}, {"Ozelot", "98", "passive.EntityOcelot"}, {"VillagerGolem", "99", "monster.EntityIronGolem"},
            {"EntityHorse", "100", "passive.EntityHorse"}, {"Villager", "120", "passive.EntityVillager"}, {"EnderCrystal", "200", "item.EntityEnderCrystal"},
    };

    static {
        // Mods read the vanilla names from these maps (Botania's shedding patterns, Thaumcraft, Mystcraft). Classes that cannot be
        // loaded here are left out.
        for (String[] e : VANILLA_ENTITIES) {
            Class<?> cls = vanillaClass(e[2]);
            if (cls == null) continue;
            int id = Integer.parseInt(e[1]);
            stringToClassMapping.put(e[0], cls);
            classToStringMapping.put(cls, e[0]);
            IDtoClassMapping.put(id, cls);
            classToIDMapping.put(cls, id);
            stringToIDMapping.put(e[0], id);
            VANILLA.add(cls);
        }
    }

    private static Class<?> vanillaClass(String relative) {
        for (String prefix : new String[] {"legacyconnector.v1_7_10.net.minecraft.entity.", "legacyconnector_synth.v1_7_10.net.minecraft.entity."}) {
            for (ClassLoader loader : new ClassLoader[] {EntityList.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
                if (loader == null) continue;
                try {
                    return Class.forName(prefix + relative, false, loader);
                } catch (ClassNotFoundException | LinkageError e) {
                    // try the next place
                }
            }
        }
        return null;
    }
    public static HashMap<Integer, Object> entityEggs = new LinkedHashMap<>() {
        @Override
        public Object put(Integer id, Object value) {
            Object old = super.put(id, value);
            if (id != null && value instanceof EntityEggInfo egg) {
                Class<?> cls = IDtoClassMapping.get(id);
                if (cls != null) mcc$egg(cls, egg);
            }
            return old;
        }
    };

    /** Called when FML registered a mod entity: an egg recorded earlier (the mod wrote the maps first) is created now. */
    public static void mcc$registered(Class<?> cls) {
        for (var e : IDtoClassMapping.entrySet()) {
            if (e.getValue() == cls && entityEggs.get(e.getKey()) instanceof EntityEggInfo egg) mcc$egg(cls, egg);
        }
    }

    private static void mcc$egg(Class<?> cls, EntityEggInfo egg) {
        LegacySpawnEggs.register(LegacyHosts.info(cls), egg.primaryColor, egg.secondaryColor);
    }

    public static void addMapping(Class<?> cls, String name, int id) {
        stringToClassMapping.put(name, cls);
        classToStringMapping.put(cls, name);
        IDtoClassMapping.put(id, cls);
        classToIDMapping.put(cls, id);
        stringToIDMapping.put(name, id);
    }

    public static void addMapping(Class<?> cls, String name, int id, int primaryColor, int secondaryColor) {
        addMapping(cls, name, id);
        entityEggs.put(id, new EntityEggInfo(id, primaryColor, secondaryColor));
    }

    public static Entity createEntityByName(String name, World world) {
        return mcc$create(stringToClassMapping.get(name), world);
    }

    public static Entity createEntityByID(int id, World world) {
        return mcc$create(getClassFromID(id), world);
    }

    public static Entity createEntityFromNBT(NBTTagCompound tag, World world) {
        Entity e = createEntityByName(tag.getString("id"), world);
        if (e != null) e.readFromNBT(tag);
        return e;
    }

    private static Entity mcc$create(Class<?> cls, World world) {
        // vanilla classes that are views of a modern entity can be created (MFR's safari nets, spawners); the others have no behaviour
        if (cls == null || VANILLA.contains(cls) && !org.kylepitcock.legacyconnector.v1_7_10.entity.VanillaEntities.isVanilla(cls)) return null;
        try {
            return (Entity) cls.getConstructor(World.class).newInstance(world);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    public static int getEntityID(Entity entity) {
        Integer id = entity == null ? null : classToIDMapping.get(entity.getClass());
        return id == null ? 0 : id;
    }

    public static Class<?> getClassFromID(int id) {
        return IDtoClassMapping.get(id);
    }

    public static String getEntityString(Entity entity) {
        return entity == null ? null : classToStringMapping.get(entity.getClass());
    }

    /** Names of every registered entity (the live key set, as in 1.7.10). */
    public static java.util.Set<String> func_151515_b() {
        return stringToClassMapping.keySet();
    }

    /** {@link #func_151515_b} under the name later mappings gave it (mods built against them call this). */
    public static java.util.Set<String> getEntityNameList() {
        return func_151515_b();
    }

    public static String getStringFromID(int id) {
        Class<?> cls = getClassFromID(id);
        return cls == null ? null : classToStringMapping.get(cls);
    }

    public static class EntityEggInfo {
        public final int spawnedID;
        public final int primaryColor;
        public final int secondaryColor;

        public EntityEggInfo(int id, int primaryColor, int secondaryColor) {
            this.spawnedID = id;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
        }
    }
}
