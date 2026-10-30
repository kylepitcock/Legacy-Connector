package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;

/** The 1.12 flying move helper (parrots): flies straight at the wanted position at the flying speed, without gravity. */
public class EntityFlyHelper extends EntityMoveHelper {
    public EntityFlyHelper(EntityLiving entity) {
        super(entity);
    }

    private double attribute(legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute attr, double fallback) {
        var instance = entity.getEntityAttribute(attr);
        return instance == null ? fallback : instance.getAttributeValue();
    }

    @Override
    public void onUpdateMoveHelper() {
        if (action == Action.MOVE_TO) {
            action = Action.WAIT;
            entity.setNoGravity(true);
            double dx = posX - entity.mcc$x();
            double dy = posY - entity.mcc$y();
            double dz = posZ - entity.mcc$z();
            double d = dx * dx + dy * dy + dz * dz;
            if (d < 2.500000277905201E-7D) {
                entity.setMoveVertical(0.0F);
                entity.setMoveForward(0.0F);
                return;
            }
            float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            Entity.mcc$set$rotationYaw(entity, limitAngle(Entity.mcc$get$rotationYaw(entity), yaw, 10.0F));
            float speedNow = Entity.mcc$get$onGround(entity)
                    ? (float) (speed * attribute(SharedMonsterAttributes.MOVEMENT_SPEED, 0.25D))
                    : (float) (speed * attribute(SharedMonsterAttributes.FLYING_SPEED, 0.4D));
            entity.setAIMoveSpeed(speedNow);
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float pitch = (float) (-(Math.atan2(dy, horizontal) * (180D / Math.PI)));
            Entity.mcc$set$rotationPitch(entity, limitAngle(Entity.mcc$get$rotationPitch(entity), pitch, 10.0F));
            entity.setMoveVertical(dy > 0.0D ? speedNow : -speedNow);
        } else {
            entity.setNoGravity(false);
            entity.setMoveVertical(0.0F);
            entity.setMoveForward(0.0F);
        }
    }
}
