package legacyconnector.v1_7_10.net.minecraft.entity.ai;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * A mount the rider steers (pigs with a carrot on a stick, mods' rideable mobs): it turns towards where the player looks and walks
 * with a speed that builds up to {@code maxSpeed}; {@link #boostSpeed} gives a short burst.
 */
public class EntityAIControlledByPlayer extends EntityAIBase {
    private final EntityLiving thisEntity;
    private final float maxSpeed;
    private float currentSpeed;
    private boolean speedBoosted;
    private int speedBoostTime;
    private int maxSpeedBoostTime;

    public EntityAIControlledByPlayer(EntityLiving entity, float maxSpeed) {
        this.thisEntity = entity;
        this.maxSpeed = maxSpeed;
        setMutexBits(7);
    }

    @Override
    public void startExecuting() {
        currentSpeed = 0.0F;
    }

    @Override
    public void resetTask() {
        speedBoosted = false;
        currentSpeed = 0.0F;
    }

    @Override
    public boolean shouldExecute() {
        return thisEntity.isEntityAlive() && Entity.mcc$get$riddenByEntity(thisEntity) instanceof EntityPlayer && (speedBoosted || thisEntity.canBeSteered());
    }

    @Override
    public void updateTask() {
        if (!(Entity.mcc$get$riddenByEntity(thisEntity) instanceof EntityPlayer player)) return;
        float turn = MathHelper.wrapAngleTo180_float(Entity.mcc$get$rotationYaw(player) - Entity.mcc$get$rotationYaw(thisEntity)) * 0.5F;
        turn = Math.max(-5.0F, Math.min(5.0F, turn));
        currentSpeed = MathHelper.clamp_float(currentSpeed + (currentSpeed < maxSpeed ? (maxSpeed - currentSpeed) * 0.01F + 0.005F : 0.0F), 0.0F, maxSpeed);
        Entity.mcc$set$rotationYaw(thisEntity, Entity.mcc$get$rotationYaw(thisEntity) + turn);
        float speed = currentSpeed;
        if (speedBoosted) {
            if (speedBoostTime++ > maxSpeedBoostTime) speedBoosted = false;
            speed += speed * 1.15F * MathHelper.sin((float) speedBoostTime / (float) maxSpeedBoostTime * (float) Math.PI);
        }
        thisEntity.setAIMoveSpeed(speed);
        thisEntity.moveEntityWithHeading(0.0F, 1.0F);
    }

    public boolean isControlledByPlayer() {
        return !isSpeedBoosted() && currentSpeed > maxSpeed * 0.3F;
    }

    public boolean isSpeedBoosted() {
        return speedBoosted;
    }

    /** Starts a speed burst of 140 to 980 ticks. */
    public void boostSpeed() {
        speedBoosted = true;
        speedBoostTime = 0;
        maxSpeedBoostTime = thisEntity.getRNG().nextInt(841) + 140;
    }
}
