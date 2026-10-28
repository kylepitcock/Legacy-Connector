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

/** 1.12.2 parrot: variant, flight, dancing to jukeboxes, mimicking mobs. The mimic sounds mods register
 * ({@link #registerMimicSound}) are the registry the layer already kept. */
public class EntityParrot extends EntityShoulderRiding {
    public EntityParrot(World world) {
        super(world);
        setSize(0.5F, 0.9F);
    }

    protected EntityParrot(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Parrot mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Parrot m ? m : null;
    }

    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(EntityParrot.class, DataSerializers.VARINT);
    public float flap;
    public float flapSpeed;
    public float oFlapSpeed;
    public float oFlap;
    public float flapping = 1.0F;
    private boolean partyParrot;

    public static void registerMimicSound(Class<? extends legacyconnector.v1_12_2.net.minecraft.entity.Entity> cls, SoundEvent sound) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.RuntimeApis1122.mcc$registerMimicSound(cls, sound);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(VARIANT, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(6.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.20000000298023224D);
    }

    public int getVariant() {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) return m.getVariant().getId();
        Integer v = dataManager.get(VARIANT);
        return v == null ? 0 : net.minecraft.util.Mth.clamp(v, 0, 4);
    }

    public void setVariant(int variant) {
        var m = mcc$self();
        if (m != null && !mcc$hosted()) m.setVariant(net.minecraft.world.entity.animal.Parrot.Variant.byId(variant));
        else dataManager.set(VARIANT, variant);
    }

    public boolean isFlying() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.isFlying() : !mcc$get$onGround(this);
    }

    public boolean isPartying() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? m.isPartyParrot() : partyParrot;
    }

    @Override
    public void setPartying(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, boolean partying) {
        partyParrot = partying;
        super.setPartying(pos, partying);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public boolean isBreedingItem(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        return false;
    }

    @Override
    public EntityParrot createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return null;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$hosted() || mcc$self() == null) tag.setInteger("Variant", getVariant());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$hosted() || mcc$self() == null) setVariant(tag.getInteger("Variant"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.PARROT_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.PARROT_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.PARROT_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/parrot");
    }
}
