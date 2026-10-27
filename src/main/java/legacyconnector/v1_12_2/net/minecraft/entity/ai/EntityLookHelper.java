package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;

/** The look helper over the mob's modern look control (ticked by the mob, unless a mod subclass replaces it). */
public class EntityLookHelper {
    protected final EntityLiving entity;
    protected float deltaLookYaw;
    protected float deltaLookPitch;
    protected boolean isLooking;
    protected double posX;
    protected double posY;
    protected double posZ;

    public EntityLookHelper(EntityLiving entity) {
        this.entity = entity;
    }

    private Mob mob() {
        return entity.mcc$entity instanceof Mob m ? m : null;
    }

    private boolean vanilla() {
        return getClass() == EntityLookHelper.class;
    }

    public void setLookPositionWithEntity(Entity target, float deltaYaw, float deltaPitch) {
        if (target == null) return;
        posX = target.mcc$x();
        posY = target instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase l ? l.mcc$y() + l.getEyeHeight() : target.mcc$y();
        posZ = target.mcc$z();
        deltaLookYaw = deltaYaw;
        deltaLookPitch = deltaPitch;
        isLooking = true;
        Mob m = mob();
        if (m != null && vanilla() && target.mcc$entity != null) m.getLookControl().setLookAt(target.mcc$entity, deltaYaw, deltaPitch);
    }

    public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
        posX = x;
        posY = y;
        posZ = z;
        deltaLookYaw = deltaYaw;
        deltaLookPitch = deltaPitch;
        isLooking = true;
        Mob m = mob();
        if (m != null && vanilla()) m.getLookControl().setLookAt(x, y, z, deltaYaw, deltaPitch);
    }

    /** Turns the head towards the look position (the modern look control does this for the vanilla helper). */
    public void onUpdateLook() {
        if (vanilla()) return;
        var l = entity;
        legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$set$rotationPitch(l, 0.0F);
        if (isLooking) {
            isLooking = false;
            double dx = posX - l.mcc$x();
            double dy = posY - (l.mcc$y() + l.getEyeHeight());
            double dz = posZ - l.mcc$z();
            double dist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            float pitch = (float) -(Math.atan2(dy, dist) * (180D / Math.PI));
            legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$set$rotationPitch(l, updateRotation(legacyconnector.v1_12_2.net.minecraft.entity.Entity.mcc$get$rotationPitch(l), pitch, deltaLookPitch));
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$set$rotationYawHead(l,
                    updateRotation(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$get$rotationYawHead(l), yaw, deltaLookYaw));
        } else {
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$set$rotationYawHead(l,
                    updateRotation(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$get$rotationYawHead(l),
                            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$get$renderYawOffset(l), 10.0F));
        }
    }

    private float updateRotation(float current, float target, float max) {
        float f = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (f > max) f = max;
        if (f < -max) f = -max;
        return current + f;
    }

    public boolean getIsLooking() {
        Mob m = mob();
        return vanilla() && m != null ? m.getLookControl().isLookingAtTarget() : isLooking;
    }

    public double getLookPosX() {
        Mob m = mob();
        return vanilla() && m != null ? m.getLookControl().getWantedX() : posX;
    }

    public double getLookPosY() {
        Mob m = mob();
        return vanilla() && m != null ? m.getLookControl().getWantedY() : posY;
    }

    public double getLookPosZ() {
        Mob m = mob();
        return vanilla() && m != null ? m.getLookControl().getWantedZ() : posZ;
    }
}
