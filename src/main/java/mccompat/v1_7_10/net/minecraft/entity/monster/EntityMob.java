package mccompat.v1_7_10.net.minecraft.entity.monster;

import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityCreature;
import mccompat.v1_7_10.net.minecraft.entity.SharedMonsterAttributes;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.util.DamageSource;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.monster.Monster;

/** 1.7.10 EntityMob: hostile old-AI behaviour (target the nearest player, melee in reach, retaliate, prefer dark). */
public abstract class EntityMob extends EntityCreature implements IMob {
    public EntityMob(World world) {
        super(world);
        this.experienceValue = 5;
    }

    protected String getSwimSound() {
        return "game.hostile.swim";
    }

    protected String getSplashSound() {
        return "game.hostile.swim.splash";
    }

    @Override
    protected Entity findPlayerToAttack() {
        EntityPlayer player = mcc$world() == null ? null : mcc$world().getClosestVulnerablePlayerToEntity(this, 16.0);
        return player != null && canEntityBeSeen(player) ? player : null;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable()) return false;
        if (!super.attackEntityFrom(source, amount)) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && attacker != this && !attacker.equals(getRidingOrRider())) entityToAttack = attacker;
        return true;
    }

    private Entity getRidingOrRider() {
        return mcc$get$ridingEntity(this);
    }

    @Override
    protected String getHurtSound() {
        return "game.hostile.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "game.hostile.die";
    }

    protected String func_146067_o(int fallDistance) {
        return fallDistance > 4 ? "game.hostile.hurt.fall.big" : "game.hostile.hurt.fall.small";
    }

    /** Melee when in reach (vanilla: within 2 blocks and overlapping vertically), once per attack cooldown. */
    @Override
    protected void attackEntity(Entity target, float distance) {
        if (attackTime <= 0 && distance < 2.0F && target.mcc$entity != null && mcc$entity != null
                && target.mcc$entity.getBoundingBox().maxY > mcc$entity.getBoundingBox().minY
                && target.mcc$entity.getBoundingBox().minY < mcc$entity.getBoundingBox().maxY) {
            attackTime = 20;
            attackEntityAsMob(target);
        }
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        return 0.5F - (mcc$world() == null ? 0.0F : mcc$world().getLightBrightness(x, y, z));
    }

    protected boolean isValidLightLevel() {
        if (!(mcc$entity != null && mcc$entity.level() instanceof ServerLevel level)) return true;
        return Monster.isDarkEnoughToSpawn(level, mcc$entity.blockPosition(), level.getRandom());
    }

    @Override
    public boolean getCanSpawnHere() {
        return mcc$entity != null && mcc$entity.level().getDifficulty() != Difficulty.PEACEFUL && isValidLightLevel() && super.getCanSpawnHere();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
    }

    protected boolean func_146066_aG() {
        return true;
    }
}
