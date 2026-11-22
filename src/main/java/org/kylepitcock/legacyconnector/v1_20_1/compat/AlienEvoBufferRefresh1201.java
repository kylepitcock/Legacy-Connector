package org.kylepitcock.legacyconnector.v1_20_1.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * GeckoLib-compatible stale-buffer predicate for AlienEvo's Palladium-backed renderer.
 *
 * The Minecraft buffer implementation details GeckoLib checks are private in
 * this mod's NeoForge compilation surface. Inspect only the exact, bytecode-audited
 * 1.21.1 wrapper names reflectively. Any missing/inaccessible member fails closed,
 * preserving the unknown consumer instead of guessing how to unwrap it.
 *
 * Reacquisition is deliberately left to the owning Palladium RenderTypeFunction.
 */
public final class AlienEvoBufferRefresh1201 {
    private static final String BUFFER_BUILDER = "com.mojang.blaze3d.vertex.BufferBuilder";
    private static final String OUTLINE_GENERATOR =
            "net.minecraft.client.renderer.OutlineBufferSource$EntityOutlineGenerator";
    private static final String DOUBLE_CONSUMER =
            "com.mojang.blaze3d.vertex.VertexMultiConsumer$Double";

    private static final Field BUILDING = findField(BUFFER_BUILDER, "building");
    private static final Method OUTLINE_DELEGATE = findMethod(OUTLINE_GENERATOR, "delegate");
    private static final Field DOUBLE_FIRST = findField(DOUBLE_CONSUMER, "first");
    private static final Field DOUBLE_SECOND = findField(DOUBLE_CONSUMER, "second");

    private AlienEvoBufferRefresh1201() {}

    /**
     * Recognizes only the direct builder, outline wrapper, and double-consumer
     * shapes GeckoLib 4.9.1 explicitly refreshes. Unknown wrappers are preserved.
     */
    public static boolean needsRefresh(VertexConsumer consumer) {
        if (consumer == null) return false;
        Set<VertexConsumer> visited =
                Collections.newSetFromMap(new IdentityHashMap<>());
        return needsRefresh(consumer, visited);
    }

    private static boolean needsRefresh(VertexConsumer consumer, Set<VertexConsumer> visited) {
        if (consumer == null || !visited.add(consumer)) return false;
        String type = consumer.getClass().getName();

        if (BUFFER_BUILDER.equals(type)) {
            Boolean building = readBoolean(BUILDING, consumer);
            return building != null && !building;
        }

        if (OUTLINE_GENERATOR.equals(type)) {
            Object delegate = invoke(OUTLINE_DELEGATE, consumer);
            return delegate instanceof VertexConsumer nested && needsRefresh(nested, visited);
        }

        if (DOUBLE_CONSUMER.equals(type)) {
            Object first = read(DOUBLE_FIRST, consumer);
            Object second = read(DOUBLE_SECOND, consumer);
            if (!(first instanceof VertexConsumer firstConsumer)
                    || !(second instanceof VertexConsumer secondConsumer)) {
                return false;
            }
            return needsRefresh(firstConsumer, visited) || needsRefresh(secondConsumer, visited);
        }

        return false;
    }

    private static Field findField(String className, String fieldName) {
        Class<?> owner = loadClass(className);
        if (owner == null) return null;
        try {
            Field field = owner.getDeclaredField(fieldName);
            return field.trySetAccessible() ? field : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static Method findMethod(String className, String methodName) {
        Class<?> owner = loadClass(className);
        if (owner == null) return null;
        try {
            Method method = owner.getDeclaredMethod(methodName);
            return method.trySetAccessible() ? method : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, AlienEvoBufferRefresh1201.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError | RuntimeException ex) {
            return null;
        }
    }

    private static Object read(Field field, Object owner) {
        if (field == null || owner == null) return null;
        try {
            return field.get(owner);
        } catch (IllegalAccessException | RuntimeException ex) {
            return null;
        }
    }

    private static Boolean readBoolean(Field field, Object owner) {
        Object value = read(field, owner);
        return value instanceof Boolean bool ? bool : null;
    }

    private static Object invoke(Method method, Object owner) {
        if (method == null || owner == null) return null;
        try {
            return method.invoke(owner);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }
}
