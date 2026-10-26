package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 skeleton: the type (0 skeleton, 1 wither skeleton) lives in watcher slot 13. */
public class EntitySkeleton extends EntityMob implements legacyconnector.v1_7_10.net.minecraft.entity.IRangedAttackMob {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntitySkeleton(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntitySkeleton(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(13, (byte) 0);
    }

    public int getSkeletonType() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton s && !mcc$hosted()) return s instanceof net.minecraft.world.entity.monster.WitherSkeleton ? 1 : 0;
        return dataWatcher.getWatchableObjectByte(13);
    }

    public void setSkeletonType(int type) {
        dataWatcher.updateObject(13, (byte) type);
        isImmuneToFire = type == 1;
        if (type == 1) setSize(0.72F, 2.34F);
        else setSize(0.6F, 1.8F);
    }

    private legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIArrowAttack aiArrowAttack;
    private legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIAttackOnCollide aiAttackOnCollide;

    /**
     * Picks the attack for what it holds: arrows with a bow, melee otherwise (a modern skeleton re-picks its own weapon goal).
     */
    public void setCombatTask() {
        if (!mcc$hosted()) {
            if (mcc$entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton s) s.reassessWeaponGoal();
            return;
        }
        if (aiArrowAttack == null) {
            aiArrowAttack = new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIArrowAttack(this, 1.0D, 20, 60, 15.0F);
            aiAttackOnCollide = new legacyconnector.v1_7_10.net.minecraft.entity.ai.EntityAIAttackOnCollide(this,
                    legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer.class, 1.2D, false);
        }
        tasks.removeTask(aiAttackOnCollide);
        tasks.removeTask(aiArrowAttack);
        var held = getHeldItem();
        if (held != null && held.getItem() == legacyconnector.v1_7_10.net.minecraft.init.Items.bow) tasks.addTask(4, aiArrowAttack);
        else tasks.addTask(4, aiAttackOnCollide);
    }

    /** Shoots an arrow at the target (stronger with Power, knocking back with Punch, burning with Flame or as a wither skeleton). */
    @Override
    public void attackEntityWithRangedAttack(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase target, float distanceFactor) {
        if (!mcc$hosted()) {
            if (mcc$entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton s && target != null
                    && target.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living) {
                s.performRangedAttack(living, distanceFactor);
            }
            return;
        }
        legacyconnector.v1_7_10.net.minecraft.world.World world = mcc$world();
        int difficulty = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$get$difficultySetting(world).getDifficultyId();
        var arrow = new legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityArrow(world, this, target, 1.6F, 14 - difficulty * 4);
        var held = getHeldItem();
        int power = legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(48, held);
        int punch = legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(49, held);
        arrow.setDamage(distanceFactor * 2.0F + rand.nextGaussian() * 0.25D + difficulty * 0.11F);
        if (power > 0) arrow.setDamage(arrow.getDamage() + power * 0.5D + 0.5D);
        if (punch > 0) arrow.setKnockbackStrength(punch);
        if (legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(50, held) > 0 || getSkeletonType() == 1) arrow.setFire(100);
        playSound("random.bow", 1.0F, 1.0F / (getRNG().nextFloat() * 0.4F + 0.8F));
        world.spawnEntityInWorld(arrow);
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }
}
