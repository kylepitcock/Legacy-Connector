package org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOD-SPECIFIC COMPAT (mod id "fiskheroes"): Fisk's Superheroes patches Minecraft with a coremod (45 class transformers) that
 * calls {@code com.fiskmods.heroes.asm.ASMHooks} from vanilla code; the transformers cannot run on the shim. The hooks are
 * plain static methods, so the ones that matter are called from here: {@code moveEntityWithHeading}, which replaces a hero's
 * walking with gliding, controlled flight (Iron Man, Superman) and wall crawling. A no-op without the mod; remove with the
 * mixin {@code LivingEntityTravelMixin}.
 */
public final class FiskHeroesHooks {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/FiskHeroes");
    private static final String MOD_ID = "fiskheroes";

    private static volatile boolean resolved;
    private static volatile boolean absent;
    private static MethodHandle moveEntityWithHeading;
    private static MethodHandle posePre;
    private static MethodHandle posePost;
    private static int failures;

    private FiskHeroesHooks() {}

    private static boolean ready() {
        if (resolved) return !absent;
        LegacyModRecord rec = LegacyMods.byLegacyId(MOD_ID);
        if (rec == null) {
            absent = true; // legacy mods are discovered before any world exists
            resolved = true;
            return false;
        }
        if (rec.mainClass == null) return false; // not constructed yet
        synchronized (FiskHeroesHooks.class) {
            if (resolved) return !absent;
            try {
                ClassLoader loader = rec.mainClass.getClassLoader();
                Class<?> hooks = Class.forName("com.fiskmods.heroes.asm.ASMHooks", true, loader);
                moveEntityWithHeading = MethodHandles.publicLookup().findStatic(hooks, "moveEntityWithHeading",
                        MethodType.methodType(boolean.class, legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase.class, float.class, float.class));
                // ModelHelper: the pose hooks its CTModelBiped patch put at the start and end of ModelBiped.setRotationAngles
                Class<?> helper = Class.forName("com.fiskmods.heroes.client.model.ModelHelper", true, loader);
                MethodType pose = MethodType.methodType(void.class, legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped.class, float.class, float.class,
                        float.class, float.class, float.class, float.class, legacyconnector.v1_7_10.net.minecraft.entity.Entity.class);
                try {
                    posePre = MethodHandles.publicLookup().findStatic(helper, "setRotationAnglesPre", pose);
                    posePost = MethodHandles.publicLookup().findStatic(helper, "setRotationAngles", pose);
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOG.debug("Fisk's Heroes' pose hooks are not available (a dedicated server): {}", e.toString());
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                LOG.warn("Fisk's Heroes' movement hook is not available: {}", e.toString());
                absent = true;
            }
            resolved = true;
            return !absent;
        }
    }

    /** Whether the hero hooks took over this client-side movement step (the travel vector is strafe, up, forward). */
    public static boolean moveEntityWithHeading(LivingEntity entity, Vec3 travel) {
        if (resolved && absent) return false;
        if (!entity.level().isClientSide || !ready()) return false;
        try {
            return (boolean) moveEntityWithHeading.invoke((legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase) EntityBridge.wrap(entity),
                    (float) travel.x, (float) travel.z);
        } catch (Throwable e) {
            if (failures++ < 3) LOG.warn("Fisk's Heroes' movement hook failed", e);
            return false;
        }
    }

    /** Whether Fisk's Heroes' model pose hooks are installed (a cheap check; false without the mod). */
    public static boolean poseHooks() {
        if (resolved) return posePre != null;
        return ready() && posePre != null;
    }

    /** ModelBiped.setRotationAngles, first thing: the hero's animation state is read from the model. */
    public static void setRotationAnglesPre(legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped model, float a, float b, float c, float d, float e, float f,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        call(posePre, model, a, b, c, d, e, f, entity);
    }

    /** ModelBiped.setRotationAngles, last thing: the hero's pose (flying, aiming, ...) replaces the vanilla angles. */
    public static void setRotationAngles(legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped model, float a, float b, float c, float d, float e, float f,
            legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        call(posePost, model, a, b, c, d, e, f, entity);
    }

    private static void call(MethodHandle handle, legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped model, float a, float b, float c, float d, float e,
            float f, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        if (handle == null) return;
        try {
            handle.invoke(model, a, b, c, d, e, f, entity);
        } catch (Throwable t) {
            if (failures++ < 3) LOG.warn("Fisk's Heroes' pose hook failed", t);
        }
    }
}
