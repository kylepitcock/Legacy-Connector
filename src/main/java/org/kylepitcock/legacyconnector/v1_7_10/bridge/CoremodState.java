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

    private CoremodState() {}

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
