package org.kylepitcock.legacyconnector.v1_7_10.compat.am2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.slf4j.LoggerFactory;

/** Client presentation and input for AM2's gravity inversion. */
public final class Am2Camera {
    private record RotationMethods(Method previous, Method current, Method flipped) {}

    private static final ClassValue<RotationMethods> ROTATION = new ClassValue<>() {
        @Override
        protected RotationMethods computeValue(Class<?> type) {
            try {
                return new RotationMethods(type.getMethod("getPrevFlipRotation"), type.getMethod("getFlipRotation"),
                        type.getMethod("getIsFlipped"));
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    };
    private static boolean failed;

    private Am2Camera() {}

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        var entity = event.getCamera().getEntity();
        if (entity != Minecraft.getInstance().player) return;
        event.setRoll(event.getRoll() + rollFor(entity, (float) event.getPartialTick()));
    }

    public static void onMovementInput(MovementInputUpdateEvent event) {
        // A 180-degree roll mirrors screen left and right. Forward and back still follow the viewing direction.
        if (rollFor(event.getEntity(), 1.0F) > 0.0F) event.getInput().leftImpulse *= -1.0F;
    }

    private static Object properties(Entity entity) {
        if (failed || !(entity instanceof net.minecraft.world.entity.player.Player)) return null;
        return EntityBridge.wrap(entity).getExtendedProperties("ArsMagicaExProps");
    }

    public static float rollFor(Entity entity, float partialTick) {
        var properties = properties(entity);
        if (properties == null) return 0.0F;
        try {
            return flipRoll(properties, partialTick);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return 0.0F;
        }
    }

    public static boolean reverseMouse(Entity entity) {
        var properties = properties(entity);
        if (properties == null) return false;
        try {
            return (boolean) ROTATION.get(properties.getClass()).flipped().invoke(properties);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    private static void fail(Throwable error) {
        failed = true;
        LoggerFactory.getLogger("LegacyConnector/AM2").warn("Could not read AM2's gravity orientation", error);
    }

    static float flipRoll(Object properties, float partialTick) throws ReflectiveOperationException {
        RotationMethods methods = ROTATION.get(properties.getClass());
        return Mth.lerp(partialTick, (float) methods.previous().invoke(properties), (float) methods.current().invoke(properties));
    }

    public static double eyeOffset(float roll, float height, float eyeHeight) {
        // At 180 degrees the head is the same distance below the center as it was above it.
        return (Math.cos(Math.toRadians(roll)) - 1.0) * (eyeHeight - height * 0.5F);
    }

    public static void rotateModel(PoseStack pose, float roll, float height) {
        float pivot = height * 0.5F;
        pose.translate(0.0F, pivot, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.translate(0.0F, -pivot, 0.0F);
    }
}
