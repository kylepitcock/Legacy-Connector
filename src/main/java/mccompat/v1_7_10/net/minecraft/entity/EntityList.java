package mccompat.v1_7_10.net.minecraft.entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.mccompat.v1_7_10.entity.LegacyHosts;
import org.kylepitcock.mccompat.v1_7_10.entity.LegacySpawnEggs;

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
        if (cls == null) return null;
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
