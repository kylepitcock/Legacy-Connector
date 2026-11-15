package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fields legacy coremods added to Minecraft classes (Factorization's {@code Chunk.constant_colliders}). The accessors a coremod added are mixed into the
 * shim class by the loader ({@code CoremodAdditionMixins}); their field reads and writes land here, kept per game object and not per shim object, because a
 * shim view can be made anew on every lookup (chunks). The state goes away with the game object.
 */
public final class CoremodState {
    private static final Map<Object, Map<String, Object>> STATE = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    /** Game object -> the game object it belongs to (a player's food data -> the player): what an added constructor argument gave the original. */
    private static final Map<Object, Object> OWNERS = new com.google.common.collect.MapMaker().weakKeys().weakValues().makeMap();

    private CoremodState() {}

    /** Records that {@code part} belongs to {@code owner} (called where the game makes the part). */
    public static void owner(Object part, Object owner) {
        if (part != null && owner != null) OWNERS.put(part, owner);
    }

    /**
     * An added object field of type {@code type} (internal name, legacy or already relocated). A coremod that adds a back reference to a part ({@code FoodStats.player}) fills
     * it through a constructor it adds and has the owner call; those are not applied, so a field nothing wrote yet reads as the part's owner when the owner
     * is of the field's type.
     */
    public static Object get(Object view, String field, String type) {
        Object value = get(view, field);
        if (value != null) return value;
        Object owner = OWNERS.get(key(view));
        if (owner == null) return null;
        try {
            Class<?> c = view.getClass();
            while (c != null && !c.getName().startsWith("legacyconnector.v")) c = c.getSuperclass();
            if (c == null) return null;
            String root = c.getName().substring(0, c.getName().indexOf('.', "legacyconnector.".length()) + 1);
            // the translator relocates the type name in the generated mixin like any class name constant; a name it left alone is a legacy one
            String name = type.replace('/', '.');
            Class<?> wanted = Class.forName(name.startsWith("legacyconnector.v") ? name : root + name, false, c.getClassLoader());
            Object legacy = owner;
            if (owner instanceof net.minecraft.world.entity.Entity e) {
                legacy = root.contains("v1_12_2") ? org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e) : EntityBridge.wrap(e);
            }
            return wanted.isInstance(legacy) ? legacy : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** The object the state belongs to: the game object behind a view that is not kept, otherwise the view itself. */
    private static Object key(Object view) {
        if (view instanceof legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk c) {
            net.minecraft.world.level.chunk.ChunkAccess modern = c.mcc$modern();
            if (modern instanceof net.minecraft.world.level.chunk.ImposterProtoChunk p) return p.getWrapped();
            return modern == null ? view : modern;
        }
        if (view instanceof legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk c) {
            net.minecraft.world.level.chunk.ChunkAccess modern = c.mcc$access();
            if (modern instanceof net.minecraft.world.level.chunk.ImposterProtoChunk p) return p.getWrapped();
            return modern == null ? view : modern;
        }
        if (view instanceof legacyconnector.v1_12_2.net.minecraft.util.FoodStats f) return f.mcc$modern();
        if (view instanceof legacyconnector.v1_7_10.net.minecraft.util.FoodStats f) return f.mcc$modern();
        return view;
    }

    public static Object get(Object view, String field) {
        Map<String, Object> fields = STATE.get(key(view));
        return fields == null ? null : fields.get(field);
    }

    public static void put(Object view, Object value, String field) {
        Object key = key(view);
        if (value == null) {
            Map<String, Object> fields = STATE.get(key);
            if (fields != null) fields.remove(field);
            return;
        }
        STATE.computeIfAbsent(key, k -> new ConcurrentHashMap<>(4)).put(field, value);
    }

    public static int getInt(Object view, String field) {
        return get(view, field) instanceof Integer v ? v : 0;
    }

    public static void putInt(Object view, int value, String field) {
        put(view, value, field);
    }

    public static long getLong(Object view, String field) {
        return get(view, field) instanceof Long v ? v : 0L;
    }

    public static void putLong(Object view, long value, String field) {
        put(view, value, field);
    }

    public static float getFloat(Object view, String field) {
        return get(view, field) instanceof Float v ? v : 0F;
    }

    public static void putFloat(Object view, float value, String field) {
        put(view, value, field);
    }

    public static double getDouble(Object view, String field) {
        return get(view, field) instanceof Double v ? v : 0D;
    }

    public static void putDouble(Object view, double value, String field) {
        put(view, value, field);
    }
}
