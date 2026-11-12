package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/**
 * Optional Pehkui bridge for 1.7.10 {@code Entity.setSize(width, height)} and player {@code eyeHeight} writes. The class that links Pehkui's API is
 * loaded only when the mod is installed. Without it, legacy size requests are ignored with one warning rather than failing class loading.
 */
public final class EntitySizes1710 {
    private static final String PEHKUI_HELPER = "org.kylepitcock.legacyconnector.v1_7_10.entity.PehkuiEntitySizes1710";
    private static final System.Logger LOGGER = System.getLogger("LegacyConnector/Test");
    private static volatile MethodHandle setSize;
    private static volatile MethodHandle setEyeHeight;
    private static volatile boolean attempted;
    private static volatile boolean warned;

    private EntitySizes1710() {}

    /** Changes the hitbox and model when Pehkui is present. */
    public static void setSize(Entity entity, float width, float height) {
        MethodHandle target = helper("setSize");
        if (target == null) return;
        try {
            invokeSetSize(target, entity, width, height);
        } catch (Throwable error) {
            rethrowFatal(error);
            warn("Pehkui rejected a legacy Entity.setSize request; entity sizing for this request was skipped.", error);
        }
    }

    /** Changes a player's camera height when Pehkui is present. */
    public static void setEyeHeight(Entity entity, float eye) {
        MethodHandle target = helper("setEyeHeight");
        if (target == null) return;
        try {
            invokeSetEyeHeight(target, entity, eye);
        } catch (Throwable error) {
            rethrowFatal(error);
            warn("Pehkui rejected a legacy eye-height request; entity sizing for this request was skipped.", error);
        }
    }

    private static MethodHandle helper(String method) {
        if (!ModList.get().isLoaded("pehkui")) {
            warn("Pehkui is not installed; legacy Entity.setSize and eyeHeight requests will be ignored.", null);
            return null;
        }
        if (!attempted) {
            synchronized (EntitySizes1710.class) {
                if (!attempted) {
                    try {
                        Class<?> helper = Class.forName(PEHKUI_HELPER, true, EntitySizes1710.class.getClassLoader());
                        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                        MethodHandle size = lookup.findStatic(helper, "setSize",
                                MethodType.methodType(void.class, Entity.class, float.class, float.class));
                        MethodHandle eye = lookup.findStatic(helper, "setEyeHeight",
                                MethodType.methodType(void.class, Entity.class, float.class));
                        setSize = size;
                        setEyeHeight = eye;
                    } catch (ReflectiveOperationException | LinkageError error) {
                        warn("Pehkui is installed but the Legacy Connector size bridge could not link its API.", error);
                    } finally {
                        attempted = true;
                    }
                }
            }
        }
        MethodHandle target = "setSize".equals(method) ? setSize : setEyeHeight;
        if (target == null) warn("Pehkui's legacy size bridge is unavailable; entity sizing for this request was skipped.", null);
        return target;
    }

    static void invokeSetSize(MethodHandle handle, Entity entity, float width, float height) throws Throwable {
        handle.invokeExact(entity, width, height);
    }

    static void invokeSetEyeHeight(MethodHandle handle, Entity entity, float eye) throws Throwable {
        handle.invokeExact(entity, eye);
    }

    private static void rethrowFatal(Throwable error) {
        if (error instanceof VirtualMachineError fatal) throw fatal;
        if (error instanceof ThreadDeath fatal) throw fatal;
    }

    private static void warn(String message, Throwable error) {
        if (warned) return;
        synchronized (EntitySizes1710.class) {
            if (warned) return;
            warned = true;
            if (error == null) LOGGER.log(System.Logger.Level.WARNING, message);
            else LOGGER.log(System.Logger.Level.WARNING, message, error);
        }
    }
}
