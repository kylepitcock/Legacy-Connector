package legacyconnector.v1_12_2.net.minecraft.entity.passive;

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

/** 1.12.2 skeleton horse (undead). */
public class EntitySkeletonHorse extends AbstractHorse {
    public EntitySkeletonHorse(World world) {
        super(world);
        setSize(1.3964844F, 1.6F);
    }

    protected EntitySkeletonHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.horse.SkeletonHorse mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.horse.SkeletonHorse m ? m : null;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
    }

    public boolean isTrap() {
        var h = mcc$self();
        return h != null && h.isTrap();
    }

    public void setTrap(boolean trap) {
        var h = mcc$self();
        if (h != null) h.setTrap(trap);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.SKELETON_HORSE_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.SKELETON_HORSE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.SKELETON_HORSE_DEATH);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntitySkeletonHorse(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/skeleton_horse");
    }
}
