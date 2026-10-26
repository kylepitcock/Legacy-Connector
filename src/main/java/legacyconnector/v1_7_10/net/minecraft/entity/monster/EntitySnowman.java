package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IRangedAttackMob;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntitySnowball;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 snow golem: throws snowballs at monsters. A plain one is the modern snow golem; mods' (Chisel's) run here. */
public class EntitySnowman extends EntityGolem implements IRangedAttackMob {
    /** View of a modern snow golem. */
    public EntitySnowman(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntitySnowman(World world) {
        super(world);
        setSize(0.4F, 1.8F);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(4.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.20000000298023224D);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        World world = mcc$world();
        EntitySnowball snowball = new EntitySnowball(world, this);
        double dx = mcc$get$posX(target) - mcc$get$posX(this);
        double dy = mcc$get$posY(target) + target.getEyeHeight() - 1.100000023841858D - mcc$get$posY(snowball);
        double dz = mcc$get$posZ(target) - mcc$get$posZ(this);
        float lift = (float) Math.sqrt(dx * dx + dz * dz) * 0.2F;
        snowball.setThrowableHeading(dx, dy + lift, dz, 1.6F, 12.0F);
        playSound("random.bow", 1.0F, 1.0F / (getRNG().nextFloat() * 0.4F + 0.8F));
        world.spawnEntityInWorld(snowball);
    }
}
