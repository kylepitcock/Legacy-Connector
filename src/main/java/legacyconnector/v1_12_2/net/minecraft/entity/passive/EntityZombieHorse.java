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

/** 1.12.2 zombie horse (undead). */
public class EntityZombieHorse extends AbstractHorse {
    public EntityZombieHorse(World world) {
        super(world);
        setSize(1.3964844F, 1.6F);
    }

    protected EntityZombieHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.horse.ZombieHorse mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.horse.ZombieHorse m ? m : null;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.ZOMBIE_HORSE_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.ZOMBIE_HORSE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.ZOMBIE_HORSE_DEATH);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityZombieHorse(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/zombie_horse");
    }
}
