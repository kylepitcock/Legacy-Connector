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

/** 1.12.2 horse: coat colour and markings packed in one variant number (colour + markings * 256). */
public class EntityHorse extends AbstractHorse {
    public EntityHorse(World world) {
        super(world);
        setSize(1.3964844F, 1.6F);
    }

    protected EntityHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.horse.Horse mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.horse.Horse m ? m : null;
    }

    private static final DataParameter<Integer> HORSE_VARIANT = EntityDataManager.createKey(EntityHorse.class, DataSerializers.VARINT);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(HORSE_VARIANT, 0);
    }

    public void setHorseVariant(int variant) {
        var h = mcc$self();
        if (h != null && !mcc$hosted()) {
            var tag = new net.minecraft.nbt.CompoundTag();
            h.saveWithoutId(tag);
            tag.putInt("Variant", variant);
            h.load(tag);
            return;
        }
        dataManager.set(HORSE_VARIANT, variant);
    }

    public int getHorseVariant() {
        var h = mcc$self();
        if (h != null && !mcc$hosted()) return h.getVariant().getId() & 255 | h.getMarkings().getId() << 8 & 65280;
        Integer v = dataManager.get(HORSE_VARIANT);
        return v == null ? 0 : v;
    }

    @Override
    public boolean wearsArmor() {
        return true;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$hosted() || mcc$self() == null) tag.setInteger("Variant", getHorseVariant());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$hosted() || mcc$self() == null) setHorseVariant(tag.getInteger("Variant"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.HORSE_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.HORSE_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.HORSE_DEATH);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityHorse(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/horse");
    }
}
