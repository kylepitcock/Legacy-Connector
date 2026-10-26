package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.IRangedAttackMob;
import legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityPotion;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10 witch: throws harming, slowness, poison and weakness potions; whether it is drinking is watcher slot 21. A plain one is the
 * modern witch; mods' witches (Botania's Gaia guardian) run here.
 */
public class EntityWitch extends EntityMob implements IRangedAttackMob {
    /** View of a modern witch. */
    public EntityWitch(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityWitch(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        getDataWatcher().addObject(21, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(26.0D);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25D);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    public void setAggressive(boolean aggressive) {
        getDataWatcher().updateObject(21, (byte) (aggressive ? 1 : 0));
    }

    public boolean getAggressive() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Witch w && !mcc$hosted()) return w.isDrinkingPotion();
        return getDataWatcher().getWatchableObjectByte(21) == 1;
    }

    @Override
    protected String getLivingSound() {
        return "mob.witch.idle";
    }

    @Override
    protected String getHurtSound() {
        return "mob.witch.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.witch.death";
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        if (getAggressive()) return;
        World world = mcc$world();
        EntityPotion potion = new EntityPotion(world, this, 32732);
        mcc$set$rotationPitch(potion, mcc$get$rotationPitch(potion) + 20.0F);
        double dx = mcc$get$posX(target) + mcc$get$motionX(target) - mcc$get$posX(this);
        double dy = mcc$get$posY(target) + target.getEyeHeight() - 1.100000023841858D - mcc$get$posY(this);
        double dz = mcc$get$posZ(target) + mcc$get$motionZ(target) - mcc$get$posZ(this);
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist >= 8.0F && !target.isPotionActive(legacyconnector.v1_7_10.net.minecraft.potion.Potion.moveSlowdown)) potion.setPotionDamage(32698);
        else if (target.getHealth() >= 8.0F && !target.isPotionActive(legacyconnector.v1_7_10.net.minecraft.potion.Potion.poison)) potion.setPotionDamage(32660);
        else if (dist <= 3.0F && !target.isPotionActive(legacyconnector.v1_7_10.net.minecraft.potion.Potion.weakness) && rand.nextFloat() < 0.25F) potion.setPotionDamage(32696);
        potion.setThrowableHeading(dx, dy + dist * 0.2F, dz, 0.75F, 8.0F);
        world.spawnEntityInWorld(potion);
    }
}
