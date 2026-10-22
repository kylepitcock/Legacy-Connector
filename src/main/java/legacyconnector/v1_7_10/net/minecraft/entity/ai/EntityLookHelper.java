package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import net.minecraft.world.entity.Mob;

/** Legacy look helper over the host's look control (ticked by the host unless a mod subclass replaces it). */
public class EntityLookHelper {
    protected final EntityLiving entity;

    public EntityLookHelper(EntityLiving entity) {
        this.entity = entity;
    }

    private Mob mob() {
        return entity.mcc$entity instanceof Mob m ? m : null;
    }

    public void setLookPositionWithEntity(Entity target, float yawSpeed, float pitchSpeed) {
        Mob m = mob();
        if (m != null && target != null && target.mcc$entity != null) m.getLookControl().setLookAt(target.mcc$entity, yawSpeed, pitchSpeed);
    }

    public void setLookPosition(double x, double y, double z, float yawSpeed, float pitchSpeed) {
        Mob m = mob();
        if (m != null) m.getLookControl().setLookAt(x, y, z, yawSpeed, pitchSpeed);
    }

    public void onUpdateLook() {
    }

    public boolean getIsLooking() {
        Mob m = mob();
        return m != null && m.getLookControl().isLookingAtTarget();
    }

    public double getLookPosX() {
        Mob m = mob();
        return m == null ? 0 : m.getLookControl().getWantedX();
    }

    public double getLookPosY() {
        Mob m = mob();
        return m == null ? 0 : m.getLookControl().getWantedY();
    }

    public double getLookPosZ() {
        Mob m = mob();
        return m == null ? 0 : m.getLookControl().getWantedZ();
    }
}
