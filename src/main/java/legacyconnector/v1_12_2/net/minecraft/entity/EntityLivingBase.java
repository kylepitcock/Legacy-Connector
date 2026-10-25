package legacyconnector.v1_12_2.net.minecraft.entity;

import legacyconnector.v1_12_2.net.minecraft.world.World;

public abstract class EntityLivingBase extends Entity {
    public EntityLivingBase(World world) {
        super(world);
    }

    public float getHealth() {
        return mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l.getHealth() : 0;
    }

    public float getMaxHealth() {
        return mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l.getMaxHealth() : 0;
    }
}
