package org.kylepitcock.legacyconnector.v1_20_1;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.kylepitcock.legacyconnector.core.StaticFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.20.1's {@code @ObjectHolder} injection (the annotation is the facade {@code ObjectHolder}; NeoForge 1.21.1 has none). Forge filled the holders
 * after each registry's event; so does this: {@link #inject} runs after every {@code RegisterEvent} and at load complete, filling each field whose entry
 * exists by now (a field is filled once). Holders of a mod's own registry (Cyclops' ingredient components, read by Common Capabilities) work because the
 * layer's registry builder registers the registry with the game ({@code BuiltInRegistries.REGISTRY}).
 */
public final class ObjectHolders1201 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Forge1201");
    private static final String DESC = "Llegacyconnector/v1_20_1/net/minecraftforge/registries/ObjectHolder;";

    /** A field to fill: where, which registry (name, or the guess from the field type), which entry. */
    private record Holder(Field field, String registry, ResourceLocation key) {}

    private static List<Holder> pending;
    private static int filled;

    private ObjectHolders1201() {}

    public static synchronized void inject() {
        try {
            if (pending == null) pending = scan();
            if (pending.isEmpty()) return;
            List<Holder> left = new ArrayList<>(pending.size());
            for (Holder h : pending) {
                if (!fill(h)) left.add(h);
            }
            pending = left;
        } catch (RuntimeException | LinkageError e) {
            // holders must never stop the start (FML would report only "uncaught parallel processing error" without the stack)
            LOG.warn("Object holder injection failed", e);
            if (pending == null) pending = List.of();
        }
    }

    /** Called at load complete: what is still unfilled stays null (as in Forge), logged once. */
    public static synchronized void finish() {
        inject();
        for (Holder h : pending) {
            LOG.debug("Object holder {}.{} ({}): nothing registered as {}", h.field.getDeclaringClass().getSimpleName(), h.field.getName(),
                    h.registry.isEmpty() ? "by type" : h.registry, h.key);
        }
        if (filled > 0 || !pending.isEmpty()) LOG.info("1.20.1 object holders: {} filled, {} without an entry", filled, pending.size());
    }

    private static List<Holder> scan() {
        List<Holder> out = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        java.util.Set<Object> seen = new java.util.HashSet<>();
        for (String id : Forge1201Runtime.modIds()) {
            var container = ModList.get().getModContainerById(id).orElse(null);
            if (container == null) continue;
            var file = container.getModInfo().getOwningFile().getFile();
            if (!seen.add(file)) continue;
            var scan = file.getScanResult();
            if (scan == null) continue;
            for (var a : scan.getAnnotations()) {
                if (!a.annotationType().getDescriptor().equals(DESC)) continue;
                String className = a.clazz().getClassName();
                try {
                    Class<?> owner = Class.forName(className, false, loader);
                    String registry = str(a.annotationData().get("registryName"));
                    String value = str(a.annotationData().get("value"));
                    if (a.memberName().equals(className) || a.memberName().isEmpty()) {
                        // class level: public static fields (without an annotation of their own) are entries of the namespace in `value`
                        for (Field f : owner.getDeclaredFields()) {
                            int m = f.getModifiers();
                            if (!Modifier.isStatic(m) || !Modifier.isPublic(m) || f.getType().isPrimitive()) continue;
                            if (hasOwnHolder(f)) continue;
                            String ns = value.isEmpty() ? id : value;
                            out.add(new Holder(f, registry, ResourceLocation.fromNamespaceAndPath(ns, f.getName().toLowerCase(Locale.ROOT))));
                        }
                    } else {
                        Field f = owner.getDeclaredField(a.memberName());
                        if (!Modifier.isStatic(f.getModifiers())) continue;
                        ResourceLocation key = ResourceLocation.tryParse(value.contains(":") ? value : id + ":" + value);
                        if (key != null) out.add(new Holder(f, registry, key));
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    LOG.warn("{}: cannot read the object holder {}.{}: {}", id, className, a.memberName(), e.toString());
                }
            }
        }
        return out;
    }

    private static boolean hasOwnHolder(Field f) {
        for (var an : f.getAnnotations()) if (an.annotationType().getName().endsWith("registries.ObjectHolder")) return true;
        return false;
    }

    private static String str(Object o) {
        return o instanceof String s ? s : "";
    }

    private static boolean fill(Holder h) {
        Registry<?> registry = registryOf(h);
        if (registry == null || !registry.containsKey(h.key)) return false;
        Object value = registry.get(h.key);
        if (value == null || !h.field.getType().isInstance(value)) return false;
        try {
            StaticFields.set(h.field, value);
            filled++;
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warn("Cannot inject {} into {}: {}", h.key, h.field, e.toString());
            return true;
        }
    }

    private static Registry<?> registryOf(Holder h) {
        if (!h.registry.isEmpty()) {
            ResourceLocation name = ResourceLocation.tryParse(h.registry);
            return name == null ? null : BuiltInRegistries.REGISTRY.getOptional(name).orElse(null);
        }
        Class<?> t = h.field.getType();
        for (Map.Entry<Class<?>, Registry<?>> e : byType().entrySet()) {
            if (e.getKey().isAssignableFrom(t)) return e.getValue();
        }
        return null;
    }

    private static Map<Class<?>, Registry<?>> byType;

    private static Map<Class<?>, Registry<?>> byType() {
        if (byType == null) {
            Map<Class<?>, Registry<?>> m = new LinkedHashMap<>();
            m.put(net.minecraft.world.level.block.Block.class, BuiltInRegistries.BLOCK);
            m.put(net.minecraft.world.item.Item.class, BuiltInRegistries.ITEM);
            m.put(net.minecraft.world.level.material.Fluid.class, BuiltInRegistries.FLUID);
            m.put(net.minecraft.world.entity.EntityType.class, BuiltInRegistries.ENTITY_TYPE);
            m.put(net.minecraft.world.level.block.entity.BlockEntityType.class, BuiltInRegistries.BLOCK_ENTITY_TYPE);
            m.put(net.minecraft.world.effect.MobEffect.class, BuiltInRegistries.MOB_EFFECT);
            m.put(net.minecraft.sounds.SoundEvent.class, BuiltInRegistries.SOUND_EVENT);
            m.put(net.minecraft.world.item.alchemy.Potion.class, BuiltInRegistries.POTION);
            m.put(net.minecraft.core.particles.ParticleType.class, BuiltInRegistries.PARTICLE_TYPE);
            m.put(net.minecraft.world.inventory.MenuType.class, BuiltInRegistries.MENU);
            m.put(net.minecraft.world.item.crafting.RecipeSerializer.class, BuiltInRegistries.RECIPE_SERIALIZER);
            m.put(net.minecraft.world.item.crafting.RecipeType.class, BuiltInRegistries.RECIPE_TYPE);
            m.put(net.minecraft.world.entity.npc.VillagerProfession.class, BuiltInRegistries.VILLAGER_PROFESSION);
            m.put(net.minecraft.world.entity.ai.village.poi.PoiType.class, BuiltInRegistries.POINT_OF_INTEREST_TYPE);
            m.put(net.minecraft.world.entity.ai.attributes.Attribute.class, BuiltInRegistries.ATTRIBUTE);
            byType = m;
        }
        return byType;
    }
}
