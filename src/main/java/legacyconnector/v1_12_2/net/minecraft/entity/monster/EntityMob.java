package legacyconnector.v1_12_2.net.minecraft.entity.monster;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityCreature;
import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 EntityMob: a hostile creature (spawns in the dark, attacks with its attack damage). */
public abstract class EntityMob extends EntityCreature implements IMob {
    public EntityMob(World world) {
        super(world);
        experienceValue = 5;
    }

    protected EntityMob(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.util.SoundCategory getSoundCategory() {
        return legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.HOSTILE;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
    }

    @Override
    public float getBlockPathWeight(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (mcc$entity == null || !mcc$hosted()) return super.getBlockPathWeight(pos);
        return 0.5F - mcc$entity.level().getMaxLocalRawBrightness(pos.mcc$modern()) / 15.0F;
    }

    /** Dark enough to spawn (the modern monster spawn light rule). */
    protected boolean isValidLightLevel() {
        if (mcc$entity == null) return true;
        var level = mcc$worldOverride != null ? mcc$worldOverride : mcc$entity.level();
        if (!(level instanceof net.minecraft.world.level.ServerLevelAccessor sla)) return true;
        return net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(sla, mcc$entity.blockPosition(), mcc$entity.getRandom());
    }

    @Override
    public boolean getCanSpawnHere() {
        if (mcc$entity == null) return true;
        var level = mcc$worldOverride != null ? mcc$worldOverride : mcc$entity.level();
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && isValidLightLevel() && super.getCanSpawnHere();
    }

    @Override
    protected boolean canDropLoot() {
        return true;
    }

    public boolean isPreventingPlayerRest(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        return true;
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getSwimSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.HOSTILE_SWIM);
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getSplashSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.HOSTILE_SPLASH);
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getHurtSound(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.HOSTILE_HURT);
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getDeathSound() {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.HOSTILE_DEATH);
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getFallSound(int height) {
        return org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(height > 4 ? net.minecraft.sounds.SoundEvents.HOSTILE_BIG_FALL
                : net.minecraft.sounds.SoundEvents.HOSTILE_SMALL_FALL);
    }
}
