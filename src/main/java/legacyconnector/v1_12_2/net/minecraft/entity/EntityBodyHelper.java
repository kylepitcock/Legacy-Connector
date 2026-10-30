package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;

/**
 * The 1.12 body rotation of a mob: the body turns with the head while the mob moves, and catches up with the head after it stood still
 * for ten ticks. Mods return their own from {@code createBodyHelper}.
 */
public class EntityBodyHelper {
    private final EntityLivingBase living;
    private int rotationTickCounter;
    private float prevRenderYawHead;

    public EntityBodyHelper(EntityLivingBase living) {
        this.living = living;
    }

    public void updateRenderAngles() {
        double dx = Entity.mcc$get$posX(living) - Entity.mcc$get$prevPosX(living);
        double dz = Entity.mcc$get$posZ(living) - Entity.mcc$get$prevPosZ(living);
        float head = EntityLivingBase.mcc$get$rotationYawHead(living);
        if (dx * dx + dz * dz > 2.500000277905201E-7D) {
            float body = Entity.mcc$get$rotationYaw(living);
            EntityLivingBase.mcc$set$renderYawOffset(living, body);
            head = computeAngleWithBound(body, head, 75.0F);
            EntityLivingBase.mcc$set$rotationYawHead(living, head);
            prevRenderYawHead = head;
            rotationTickCounter = 0;
            return;
        }
        var passengers = living.getPassengers();
        if (!passengers.isEmpty() && passengers.get(0) instanceof EntityLiving) return;
        float max = 75.0F;
        if (Math.abs(head - prevRenderYawHead) > 15.0F) {
            rotationTickCounter = 0;
            prevRenderYawHead = head;
        } else {
            ++rotationTickCounter;
            if (rotationTickCounter > 10) max = Math.max(1.0F - (rotationTickCounter - 10) / 10.0F, 0.0F) * 75.0F;
        }
        EntityLivingBase.mcc$set$renderYawOffset(living, computeAngleWithBound(head, EntityLivingBase.mcc$get$renderYawOffset(living), max));
    }

    private float computeAngleWithBound(float target, float current, float max) {
        float f = MathHelper.wrapDegrees(target - current);
        if (f < -max) f = -max;
        if (f >= max) f = max;
        return target - f;
    }
}
