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
    private static MethodHandle gravity;
    private static MethodHandle healRate;
    private static MethodHandle invisibleToMobs;
    private static java.lang.reflect.Field tickrateMspt;
    private static int failures;
    private static float lastTickrate = 20.0F;

    private FiskHeroesHooks() {}

    /** The hooks that run on the server too: slow motion changes the server's tick rate (the hero's TickrateHandler computes it). */
    public static void register(net.neoforged.bus.api.IEventBus gameBus) {
        gameBus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> serverTick(e.getServer()));
    }

    /**
     * Fisk's coremod made the server's main loop sleep {@code TickrateHandler.getModifiedTickRate(50)} ms; speedsters in slow motion
     * (and /forceslow) slowed the world down that way. The game has a tick rate manager that tells the clients too, so the rate
     * the handler computed ({@code mspt}) is handed to it.
     */
    private static void serverTick(net.minecraft.server.MinecraftServer server) {
        if (resolved && absent) return;
        if (!ready() || tickrateMspt == null) return;
        try {
            long mspt = tickrateMspt.getLong(null);
            if (mspt <= 0) return;
            float rate = Math.max(1.0F, Math.min(100.0F, 1000.0F / mspt));
            if (Math.abs(rate - lastTickrate) > 0.01F) {
                lastTickrate = rate;
                server.tickRateManager().setTickRate(rate);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (failures++ < 3) LOG.warn("Fisk's Heroes' tick rate hook failed", e);
        }
    }

    /** {@code ASMHooks.getEntityGravity}: Moon and Quantum Realm gravity, and gravity manipulation. Multiplies the entity's gravity. */
    public static double gravity(net.minecraft.world.entity.Entity entity, double result) {
        if (resolved && absent) return result;
        if (result == 0.0 || !ready() || gravity == null) return result;
        try {
            return (double) gravity.invoke(result, EntityBridge.wrap(entity));
        } catch (Throwable e) {
            if (failures++ < 3) LOG.warn("Fisk's Heroes' gravity hook failed", e);
            return result;
        }
    }

    /** {@code ASMHooks.getHealRate}: the number of ticks between natural heals (regeneration powers scale it). */
    public static int healRate(net.minecraft.world.entity.player.Player player, int rate) {
        if (resolved && absent) return rate;
        if (!ready() || healRate == null || player.level().isClientSide) return rate;
        try {
            return (int) healRate.invoke((legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer) EntityBridge.wrap(player), rate);
        } catch (Throwable e) {
            if (failures++ < 3) LOG.warn("Fisk's Heroes' heal rate hook failed", e);
            return rate;
        }
    }

    /** {@code SHHelper.isInvisibleToMobs}: invisible, stealthy or tiny heroes are not targeted by mobs (it replaced two target searches). */
    public static boolean invisibleToMobs(net.minecraft.world.entity.LivingEntity target, double distanceSq) {
        if (resolved && absent) return false;
        if (!(target instanceof net.minecraft.world.entity.player.Player) || !ready() || invisibleToMobs == null) return false;
        try {
            return (boolean) invisibleToMobs.invoke(EntityBridge.wrap(target), distanceSq);
        } catch (Throwable e) {
            if (failures++ < 3) LOG.warn("Fisk's Heroes' mob visibility hook failed", e);
            return false;
        }
    }

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
                gravity = optional(hooks, "getEntityGravity", MethodType.methodType(double.class, double.class, legacyconnector.v1_7_10.net.minecraft.entity.Entity.class));
                healRate = optional(hooks, "getHealRate",
                        MethodType.methodType(int.class, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, int.class));
                invisibleToMobs = optional(Class.forName("com.fiskmods.heroes.util.SHHelper", true, loader), "isInvisibleToMobs",
                        MethodType.methodType(boolean.class, legacyconnector.v1_7_10.net.minecraft.entity.Entity.class, double.class));
                try {
                    Class<?> tick = Class.forName("com.fiskmods.heroes.common.event.TickrateHandler", true, loader);
                    tickrateMspt = tick.getDeclaredField("mspt");
                    tickrateMspt.setAccessible(true);
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    LOG.debug("Fisk's Heroes' tick rate handler is not available: {}", e.toString());
                }
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

    private static MethodHandle optional(Class<?> owner, String name, MethodType type) {
        try {
            return MethodHandles.publicLookup().findStatic(owner, name, type);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.warn("Fisk's Heroes' hook {}.{} is not available: {}", owner.getSimpleName(), name, e.toString());
            return null;
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
