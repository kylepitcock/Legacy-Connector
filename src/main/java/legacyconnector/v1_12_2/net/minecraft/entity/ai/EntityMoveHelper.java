package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.entity.Mob;

/**
 * The move helper. The vanilla helper stands for the mob's modern move control (which the mob ticks); a mod's subclass keeps the
 * 1.12 state ({@code posX}, {@code speed}, {@code action}) and is ticked by the AI step, with the 1.12 ground movement as its super
 * implementation.
 */
public class EntityMoveHelper {
    protected final EntityLiving entity;
    protected double posX;
    protected double posY;
    protected double posZ;
    protected double speed;
    protected float moveForward;
    protected float moveStrafe;
    public Action action = Action.WAIT;

    public EntityMoveHelper(EntityLiving entity) {
        this.entity = entity;
    }

    private Mob mob() {
        return entity.mcc$entity instanceof Mob m ? m : null;
    }

    private boolean vanilla() {
        return getClass() == EntityMoveHelper.class;
    }

    public boolean isUpdating() {
        Mob m = mob();
        return vanilla() && m != null ? m.getMoveControl().hasWanted() : action == Action.MOVE_TO;
    }

    public double getSpeed() {
        Mob m = mob();
        return vanilla() && m != null ? m.getMoveControl().getSpeedModifier() : speed;
    }

    public void setMoveTo(double x, double y, double z, double speedIn) {
        posX = x;
        posY = y;
        posZ = z;
        speed = speedIn;
        action = Action.MOVE_TO;
        Mob m = mob();
        if (m != null && vanilla()) m.getMoveControl().setWantedPosition(x, y, z, speedIn);
    }

    public void strafe(float forward, float strafe) {
        action = Action.STRAFE;
        moveForward = forward;
        moveStrafe = strafe;
        speed = 0.25D;
        Mob m = mob();
        if (m != null && vanilla()) m.getMoveControl().strafe(forward, strafe);
    }

    public void read(EntityMoveHelper that) {
        action = that.action;
        posX = that.posX;
        posY = that.posY;
        posZ = that.posZ;
        speed = Math.max(that.speed, 1.0D);
        moveForward = that.moveForward;
        moveStrafe = that.moveStrafe;
    }

    /** The 1.12 ground movement towards the wanted position (the modern move control runs it for the vanilla helper). */
    public void onUpdateMoveHelper() {
        if (vanilla()) return;
        var attr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        double base = attr == null ? 0.25D : attr.getAttributeValue();
        if (action == Action.STRAFE) {
            float f = (float) base;
            float f1 = (float) speed * f;
            float forward = moveForward;
            float strafe = moveStrafe;
            float f4 = (float) Math.sqrt(forward * forward + strafe * strafe);
            if (f4 < 1.0F) f4 = 1.0F;
            f4 = f1 / f4;
            forward *= f4;
            strafe *= f4;
            entity.setAIMoveSpeed(f1);
            entity.setMoveForward(moveForward);
            entity.setMoveStrafing(moveStrafe);
            action = Action.WAIT;
        } else if (action == Action.MOVE_TO) {
            action = Action.WAIT;
            double dx = posX - entity.mcc$x();
            double dz = posZ - entity.mcc$z();
            double dy = posY - entity.mcc$y();
            double d = dx * dx + dy * dy + dz * dz;
            if (d < 2.500000277905201E-7D) {
                entity.setMoveForward(0.0F);
                return;
            }
            float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            EntityLivingBase.mcc$set$rotationYaw(entity, limitAngle(EntityLivingBase.mcc$get$rotationYaw(entity), yaw, 90.0F));
            entity.setAIMoveSpeed((float) (speed * base));
            if (dy > EntityLivingBase.mcc$get$stepHeight(entity) && dx * dx + dz * dz < Math.max(1.0F, EntityLivingBase.mcc$get$width(entity))) {
                entity.getJumpHelper().setJumping();
                action = Action.JUMPING;
            }
        } else if (action == Action.JUMPING) {
            entity.setAIMoveSpeed((float) (speed * base));
            if (EntityLivingBase.mcc$get$onGround(entity)) action = Action.WAIT;
        } else {
            entity.setMoveForward(0.0F);
        }
    }

    protected float limitAngle(float current, float target, float max) {
        float f = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (f > max) f = max;
        if (f < -max) f = -max;
        float out = current + f;
        if (out < 0.0F) out += 360.0F;
        else if (out > 360.0F) out -= 360.0F;
        return out;
    }

    public double getX() {
        Mob m = mob();
        return vanilla() && m != null ? m.getMoveControl().getWantedX() : posX;
    }

    public double getY() {
        Mob m = mob();
        return vanilla() && m != null ? m.getMoveControl().getWantedY() : posY;
    }

    public double getZ() {
        Mob m = mob();
        return vanilla() && m != null ? m.getMoveControl().getWantedZ() : posZ;
    }

    public enum Action {
        WAIT, MOVE_TO, STRAFE, JUMPING
    }
}
