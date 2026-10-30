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

/** 1.12.2 llama: variant (coat), strength (inventory size), spit attack, carpet decoration. */
public class EntityLlama extends AbstractChestHorse {
    public EntityLlama(World world) {
        super(world);
        setSize(1.3964844F, 1.6F);
    }

    protected EntityLlama(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.horse.Llama mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.horse.Llama m ? m : null;
    }

    private static final DataParameter<Integer> DATA_STRENGTH_ID = EntityDataManager.createKey(EntityLlama.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DATA_VARIANT_ID = EntityDataManager.createKey(EntityLlama.class, DataSerializers.VARINT);

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(DATA_STRENGTH_ID, 0);
        dataManager.register(DATA_VARIANT_ID, 0);
    }

    public int getVariant() {
        var l = mcc$self();
        if (l != null && !mcc$hosted()) return l.getVariant().getId();
        Integer v = dataManager.get(DATA_VARIANT_ID);
        return v == null ? 0 : net.minecraft.util.Mth.clamp(v, 0, 3);
    }

    public void setVariant(int variant) {
        var l = mcc$self();
        if (l != null && !mcc$hosted()) {
            l.setVariant(net.minecraft.world.entity.animal.horse.Llama.Variant.byId(variant));
            return;
        }
        dataManager.set(DATA_VARIANT_ID, variant);
    }

    public int getStrength() {
        var l = mcc$self();
        if (l != null && !mcc$hosted()) return l.getStrength();
        Integer s = dataManager.get(DATA_STRENGTH_ID);
        return s == null ? 0 : s;
    }

    public void setStrength(int strength) {
        dataManager.set(DATA_STRENGTH_ID, Math.max(1, Math.min(5, strength)));
    }

    @Override
    public int getInventoryColumns() {
        return getStrength();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$hosted() || mcc$self() == null) {
            tag.setInteger("Variant", getVariant());
            tag.setInteger("Strength", getStrength());
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$hosted() || mcc$self() == null) {
            setStrength(tag.getInteger("Strength"));
            setVariant(tag.getInteger("Variant"));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.LLAMA_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.LLAMA_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.LLAMA_DEATH);
    }

    @Override
    public EntityLlama createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntityLlama(mcc$get$world(this));
    }

    /** The color of the carpet the llama wears, or null. */
    public legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor getColor() {
        var l = mcc$self();
        if (l != null && !mcc$hosted()) {
            var swag = l.getSwag();
            return swag == null ? null : legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(swag.getId());
        }
        if (horseChest == null || horseChest.getSizeInventory() < 2) return null;
        var decor = horseChest.getStackInSlot(1);
        if (decor.isEmpty() || !(decor.getItem().mcc$modern instanceof net.minecraft.world.item.BlockItem b)
                || !(b.getBlock() instanceof net.minecraft.world.level.block.WoolCarpetBlock carpet)) {
            return null;
        }
        return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(carpet.getColor().getId());
    }

    public boolean hasColor() {
        return getColor() != null;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/llama");
    }
}
