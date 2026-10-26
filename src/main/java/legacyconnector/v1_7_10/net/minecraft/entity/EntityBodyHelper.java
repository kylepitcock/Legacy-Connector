package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * Turns a living entity's body after its head: while it moves the body faces its yaw (the head at most 75 degrees off), while it
 * stands the body follows the head, easing off after ten still ticks (Railcraft's trade station spins its villager with it).
 */
public class EntityBodyHelper {
    private final EntityLivingBase theLiving;
    private int rotationTickCounter;
    private float prevRenderYawHead;

    public EntityBodyHelper(EntityLivingBase living) {
        this.theLiving = living;
    }

    public void updateRenderAngles() {
        EntityLivingBase e = theLiving;
        double dx = Entity.mcc$get$posX(e) - Entity.mcc$get$prevPosX(e);
        double dz = Entity.mcc$get$posZ(e) - Entity.mcc$get$prevPosZ(e);
        float head = EntityLivingBase.mcc$get$rotationYawHead(e);
        if (dx * dx + dz * dz > 2.500000277905201E-7D) {
            float body = Entity.mcc$get$rotationYaw(e);
            EntityLivingBase.mcc$set$renderYawOffset(e, body);
            head = computeAngleWithBound(body, head, 75.0F);
            EntityLivingBase.mcc$set$rotationYawHead(e, head);
            prevRenderYawHead = head;
            rotationTickCounter = 0;
            return;
        }
        float bound = 75.0F;
        if (Math.abs(head - prevRenderYawHead) > 15.0F) {
            rotationTickCounter = 0;
            prevRenderYawHead = head;
        } else {
            ++rotationTickCounter;
            if (rotationTickCounter > 10) bound = Math.max(1.0F - (rotationTickCounter - 10) / 10.0F, 0.0F) * 75.0F;
        }
        EntityLivingBase.mcc$set$renderYawOffset(e, computeAngleWithBound(head, EntityLivingBase.mcc$get$renderYawOffset(e), bound));
    }

    /** {@code target}, moved towards {@code current} so the two are at most {@code max} degrees apart. */
    private float computeAngleWithBound(float target, float current, float max) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        if (diff < -max) diff = -max;
        if (diff >= max) diff = max;
        return target - diff;
    }
}
