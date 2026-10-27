package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Entity names: registered entries by class, vanilla entity classes by their class name ("EntityCow" is "minecraft:cow"). */
public class EntityList {
    /** Spawn egg colours by entity name (filled when an entry gets its egg). */
    public static final java.util.Map<ResourceLocation, EntityEggInfo> ENTITY_EGGS = new java.util.concurrent.ConcurrentHashMap<>();

    /** A spawn egg's entity and colours. */
    public static class EntityEggInfo {
        public final ResourceLocation spawnedID;
        public final int primaryColor;
        public final int secondaryColor;

        public EntityEggInfo(ResourceLocation id, int primaryColor, int secondaryColor) {
            this.spawnedID = id;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
        }
    }

    public static ResourceLocation getKey(Class<? extends Entity> cls) {
        for (EntityEntry e : ForgeRegistries.ENTITIES.getValues()) {
            // vanilla entries stand on shared base classes (EntityLiving), which name no entity
            if (e.getEntityClass() == cls && !"minecraft".equals(e.getRegistryName().getNamespace())) return e.getRegistryName();
        }
        String n = cls.getSimpleName();
        if (n.startsWith("Entity")) n = n.substring("Entity".length());
        return n.isEmpty() ? null : new ResourceLocation("minecraft", n.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT));
    }

    public static ResourceLocation getKey(Entity entity) {
        return getKey(entity.getClass());
    }

    public static String getEntityString(Entity entity) {
        ResourceLocation key = getKey(entity);
        return key == null ? null : key.toString();
    }

    public static Class<? extends Entity> getClass(ResourceLocation name) {
        EntityEntry e = ForgeRegistries.ENTITIES.getValue(name);
        return e == null ? null : e.getEntityClass();
    }

    public static java.util.Set<ResourceLocation> getEntityNameList() {
        return ForgeRegistries.ENTITIES.getKeys();
    }

    public static boolean isRegistered(ResourceLocation name) {
        return name != null && ForgeRegistries.ENTITIES.containsKey(name);
    }

    public static String getTranslationName(ResourceLocation name) {
        EntityEntry e = name == null ? null : ForgeRegistries.ENTITIES.getValue(name);
        return e == null ? null : e.getName();
    }

    // ------------------------------------------------------------------ creating entities by name, id and NBT (area E, 1.12.2)

    /** 1.12 vanilla entity names the 1.13+ renames changed (legacy -> modern). */
    private static final java.util.Map<String, String> RENAMES = java.util.Map.ofEntries(java.util.Map.entry("zombie_pigman", "zombified_piglin"),
            java.util.Map.entry("snowman", "snow_golem"), java.util.Map.entry("villager_golem", "iron_golem"),
            java.util.Map.entry("commandblock_minecart", "command_block_minecart"), java.util.Map.entry("xp_orb", "experience_orb"),
            java.util.Map.entry("xp_bottle", "experience_bottle"), java.util.Map.entry("ender_crystal", "end_crystal"),
            java.util.Map.entry("evocation_illager", "evoker"), java.util.Map.entry("vindication_illager", "vindicator"),
            java.util.Map.entry("illusion_illager", "illusioner"), java.util.Map.entry("evocation_fangs", "evoker_fangs"),
            java.util.Map.entry("fireworks_rocket", "firework_rocket"), java.util.Map.entry("eye_of_ender_signal", "eye_of_ender"));

    /**
     * A new entity of a registered name, not yet spawned: a mod entity through its modern type (its host constructs the legacy object),
     * a vanilla one as a view of a new modern entity. Null for unknown names or without a real world.
     */
    public static Entity createEntityByIDFromName(ResourceLocation name, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        if (name == null || world == null || world.mcc$level() == null) return null;
        var level = world.mcc$level();
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(name);
        if (entry != null && !"minecraft".equals(name.getNamespace())) {
            var info = org.kylepitcock.legacyconnector.v1_12_2.entity.Hosts1122.info(entry.getEntityClass());
            if (info != null) {
                var modern = info.type().create(level);
                return modern == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern);
            }
            try {
                return entry.newInstance(world);
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }
        var type = mcc$modernType(name);
        var modern = type == null ? null : type.create(level);
        return modern == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern);
    }

    /** The modern type of a vanilla 1.12 entity name (renamed ids included), or null. */
    public static net.minecraft.world.entity.EntityType<?> mcc$modernType(ResourceLocation name) {
        if (name == null) return null;
        String path = RENAMES.getOrDefault(name.getPath(), name.getPath());
        var id = net.minecraft.resources.ResourceLocation.tryBuild(name.getNamespace(), path);
        return id == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    /** An entity made from its saved NBT ({@code id} names the type), with the rest of the tag read into it. */
    public static Entity createEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        if (tag == null) return null;
        Entity entity = createEntityByIDFromName(new ResourceLocation(tag.getString("id")), world);
        if (entity == null) return null;
        try {
            entity.readFromNBT(tag);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("Entity {} failed to read its NBT: {}", tag.getString("id"), e.toString());
            return null;
        }
        return entity;
    }

    /** By the numeric id of its registry entry. */
    public static Entity createEntityByID(int id, legacyconnector.v1_12_2.net.minecraft.world.World world) {
        EntityEntry e = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENTITIES.getValue(id);
        return e == null ? null : createEntityByIDFromName(e.getRegistryName(), world);
    }

    /** The numeric id of the class's registry entry, or -1. */
    public static int getID(Class<? extends Entity> cls) {
        for (EntityEntry e : ForgeRegistries.ENTITIES.getValues()) {
            if (e.getEntityClass() == cls && !"minecraft".equals(e.getRegistryName().getNamespace())) {
                return org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENTITIES.getID(e);
            }
        }
        return -1;
    }

    public static Class<? extends Entity> getClassFromID(int id) {
        EntityEntry e = org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122.ENTITIES.getValue(id);
        return e == null ? null : e.getEntityClass();
    }

    /** The class of a name ("minecraft:pig" or an old bare name such as "Pig"). */
    public static Class<? extends Entity> getClassFromName(String name) {
        if (name == null || name.isEmpty()) return null;
        String n = name.indexOf(':') < 0 && !name.equals(name.toLowerCase(java.util.Locale.ROOT))
                ? name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT) : name;
        return getClass(new ResourceLocation(n));
    }
}
