package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.EntityEntry;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Entity names: registered entries by class, vanilla entity classes by their class name ("EntityCow" is "minecraft:cow"). */
public class EntityList {
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
}
