package legacyconnector.v1_12_2.net.minecraft.entity.monster;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 magma cube: a fire-immune slime of the Nether. */
public class EntityMagmaCube extends EntitySlime {
    public EntityMagmaCube(World world) {
        super(world);
        setSize(0.51000005F, 0.51000005F);
    }

    protected EntityMagmaCube(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.MagmaCube mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.MagmaCube m ? m : null;
    }

    @Override
    public boolean isImmuneToFire() {
        return true;
    }

    @Override
    protected EntitySlime createInstance() {
        return new EntityMagmaCube(mcc$get$world(this));
    }

    @Override
    protected int getAttackStrength() {
        return super.getAttackStrength() + 2;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(isSmallSlime() ? SoundEvents.MAGMA_CUBE_HURT_SMALL : SoundEvents.MAGMA_CUBE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(isSmallSlime() ? SoundEvents.MAGMA_CUBE_DEATH_SMALL : SoundEvents.MAGMA_CUBE_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return isSmallSlime() ? null : new ResourceLocation("minecraft", "entities/magma_cube");
    }
}
