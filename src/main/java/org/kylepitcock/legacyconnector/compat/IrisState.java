package org.kylepitcock.legacyconnector.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Iris's state, read reflectively so Legacy Connector does not depend on Iris: whether a shader pack renders right now
 * ({@code IrisApi.getInstance().isShaderPackInUse()}), whether the shadow pass is being drawn ({@code isRenderingShadowPass()}),
 * and the class Iris's batched entity rendering sorts a render type into ({@code BlendingStateHolder}). Everything answers
 * "no Iris" when Iris is not installed or its API cannot be reached.
 */
public final class IrisState {
    private static volatile boolean resolved;
    private static MethodHandle inUse, shadowPass;
    private static Class<?> blendingHolder;
    private static MethodHandle getTransparency, setTransparency, transparencyValueOf;
    /** {@code render.irisCompat}: the adaptations of legacy rendering to a shader pack (see docs/wiki/Rendering.md). */
    private static final boolean COMPAT = org.kylepitcock.legacyconnector.core.LegacyConfig.renderIrisCompat();

    private IrisState() {}

    public static Boolean shaderPackInUse() {
        if (!resolved) resolve();
        if (inUse == null) return null;
        try {
            return (boolean) inUse.invoke();
        } catch (Throwable t) {
            return null;
        }
    }

    /** A shader pack is in use and legacy rendering adapts to it ({@code render.irisCompat}). */
    public static boolean compatPack() {
        if (!COMPAT) return false;
        Boolean b = shaderPackInUse();
        return b != null && b;
    }

    /**
     * Iris draws its shadow map right now (entities and block entities, legacy renderers included, are drawn a second time
     * from the sun) and legacy rendering adapts to it ({@code render.irisCompat}). Only meaningful on the render thread.
     */
    public static boolean compatShadowPass() {
        if (!COMPAT) return false;
        if (!resolved) resolve();
        if (shadowPass == null) return false;
        try {
            return (boolean) shadowPass.invoke();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Puts a render type into one of the classes Iris's batched entity rendering draws in order (OPAQUE, OPAQUE_DECAL,
     * GENERAL_TRANSPARENT, DECAL, WATER_MASK, LINES): Iris classifies by the vanilla state shards it knows, so a type with a
     * state shard of its own fell into the general class. No-op without Iris.
     */
    public static void setBatchingClass(Object renderType, String transparencyType) {
        if (!resolved) resolve();
        if (setTransparency == null || !blendingHolder.isInstance(renderType)) return;
        try {
            setTransparency.invoke(renderType, transparencyValueOf.invoke(transparencyType));
        } catch (Throwable t) {
            // older or newer Iris without this class: its own classification stays
        }
    }

    /** Gives a render type that wraps another the class Iris's batched rendering gave the wrapped one. */
    public static void copyBatchingClass(Object from, Object to) {
        if (!resolved) resolve();
        if (getTransparency == null || !blendingHolder.isInstance(from) || !blendingHolder.isInstance(to)) return;
        try {
            setTransparency.invoke(to, getTransparency.invoke(from));
        } catch (Throwable t) {
            // keep Iris's own classification
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            inUse = lookup.findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
            try {
                shadowPass = lookup.findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class)).bindTo(instance);
            } catch (ReflectiveOperationException e) {
                shadowPass = null;
            }
        } catch (Throwable t) {
            inUse = null; // no Iris
            shadowPass = null;
        }
        try {
            Class<?> holder = Class.forName("net.irisshaders.batchedentityrendering.impl.BlendingStateHolder");
            Class<?> type = Class.forName("net.irisshaders.batchedentityrendering.impl.TransparencyType");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle get = lookup.findVirtual(holder, "getTransparencyType", MethodType.methodType(type))
                    .asType(MethodType.methodType(Object.class, Object.class));
            MethodHandle set = lookup.findVirtual(holder, "setTransparencyType", MethodType.methodType(void.class, type))
                    .asType(MethodType.methodType(void.class, Object.class, Object.class));
            MethodHandle valueOf = lookup.findStatic(type, "valueOf", MethodType.methodType(type, String.class))
                    .asType(MethodType.methodType(Object.class, String.class));
            blendingHolder = holder;
            getTransparency = get;
            setTransparency = set;
            transparencyValueOf = valueOf;
        } catch (Throwable t) {
            setTransparency = null; // no Iris, or its batching changed
            getTransparency = null;
        }
        resolved = true;
    }
}
