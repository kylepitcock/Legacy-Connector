package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult;

/** 1.12.2 projectile helpers: the next tick's hit along the motion, and turning toward the motion. */
public final class ProjectileHelper {
    private ProjectileHelper() {}

    /**
     * What the projectile hits moving along its motion this tick: the nearest collidable entity (not {@code ignoreEntity}, nor the shooter
     * for its first 25 ticks unless {@code ignoreExcludedEntity}) or the block in the way; null when nothing.
     */
    public static RayTraceResult forwardsRaycast(Entity projectile, boolean includeEntities, boolean ignoreExcludedEntity, Entity excludedEntity) {
        var self = projectile == null ? null : projectile.mcc$entity;
        if (self == null) return null;
        var level = self.level();
        var from = self.position();
        var motion = self.getDeltaMovement();
        var to = from.add(motion);
        net.minecraft.world.phys.HitResult hit = level.clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, self));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) to = hit.getLocation();
        else hit = null;
        if (includeEntities) {
            net.minecraft.world.entity.Entity nearest = null;
            double best = 0.0D;
            int age = self.tickCount;
            for (var candidate : level.getEntities(self, self.getBoundingBox().expandTowards(motion).inflate(1.0D))) {
                var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(candidate);
                if (view == null || !view.canBeCollidedWith()) continue;
                if (!ignoreExcludedEntity && view == excludedEntity && age < 25) continue;
                if (ignoreExcludedEntity && view == excludedEntity) continue;
                var intercept = candidate.getBoundingBox().inflate(0.30000001192092896D).clip(from, to);
                if (intercept.isPresent()) {
                    double d = from.distanceToSqr(intercept.get());
                    if (d < best || best == 0.0D) {
                        nearest = candidate;
                        best = d;
                    }
                }
            }
            if (nearest != null) hit = new net.minecraft.world.phys.EntityHitResult(nearest);
        }
        return hit == null ? null : RayTraceResult.mcc$of(hit);
    }

    /** Turns yaw and pitch a share of the way toward the direction of motion (verbatim 1.12). */
    public static void rotateTowardsMovement(Entity projectile, float rotationSpeed) {
        double mx = Entity.mcc$get$motionX(projectile);
        double my = Entity.mcc$get$motionY(projectile);
        double mz = Entity.mcc$get$motionZ(projectile);
        float f = MathHelper.sqrt(mx * mx + mz * mz);
        float yaw = (float) (MathHelper.atan2(mz, mx) * (180D / Math.PI)) + 90.0F;
        float pitch = (float) (MathHelper.atan2(f, my) * (180D / Math.PI)) - 90.0F;
        Entity.mcc$set$rotationYaw(projectile, yaw);
        Entity.mcc$set$rotationPitch(projectile, pitch);
        while (pitch - Entity.mcc$get$prevRotationPitch(projectile) < -180.0F) Entity.mcc$set$prevRotationPitch(projectile, Entity.mcc$get$prevRotationPitch(projectile) - 360.0F);
        while (pitch - Entity.mcc$get$prevRotationPitch(projectile) >= 180.0F) Entity.mcc$set$prevRotationPitch(projectile, Entity.mcc$get$prevRotationPitch(projectile) + 360.0F);
        while (yaw - Entity.mcc$get$prevRotationYaw(projectile) < -180.0F) Entity.mcc$set$prevRotationYaw(projectile, Entity.mcc$get$prevRotationYaw(projectile) - 360.0F);
        while (yaw - Entity.mcc$get$prevRotationYaw(projectile) >= 180.0F) Entity.mcc$set$prevRotationYaw(projectile, Entity.mcc$get$prevRotationYaw(projectile) + 360.0F);
        float prevPitch = Entity.mcc$get$prevRotationPitch(projectile);
        float prevYaw = Entity.mcc$get$prevRotationYaw(projectile);
        Entity.mcc$set$rotationPitch(projectile, prevPitch + (pitch - prevPitch) * rotationSpeed);
        Entity.mcc$set$rotationYaw(projectile, prevYaw + (yaw - prevYaw) * rotationSpeed);
    }
}
