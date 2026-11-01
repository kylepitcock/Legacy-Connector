package org.kylepitcock.legacyconnector.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Whether Iris renders with a shader pack right now ({@code IrisApi.getInstance().isShaderPackInUse()}), read reflectively so
 * Legacy Connector does not depend on Iris. Null when Iris is not installed or its API cannot be reached.
 */
public final class IrisState {
    private static volatile boolean resolved;
    private static MethodHandle inUse;

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

    private static synchronized void resolve() {
        if (resolved) return;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            MethodHandle h = MethodHandles.publicLookup().findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class));
            inUse = h.bindTo(instance);
        } catch (Throwable t) {
            inUse = null; // no Iris
        }
        resolved = true;
    }
}
