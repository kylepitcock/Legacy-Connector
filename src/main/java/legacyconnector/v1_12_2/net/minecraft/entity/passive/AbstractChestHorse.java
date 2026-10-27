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

/** 1.12.2 horse that can carry a chest (donkeys, mules, llamas). */
public abstract class AbstractChestHorse extends AbstractHorse {
    private static final DataParameter<Boolean> DATA_ID_CHEST = EntityDataManager.createKey(AbstractChestHorse.class, DataSerializers.BOOLEAN);

    public AbstractChestHorse(World world) {
        super(world);
        canGallop = false;
    }

    protected AbstractChestHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.animal.horse.AbstractChestedHorse mcc$chested() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.animal.horse.AbstractChestedHorse h ? h : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(DATA_ID_CHEST, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.17499999701976776D);
        getEntityAttribute(JUMP_STRENGTH).setBaseValue(0.5D);
    }

    public boolean hasChest() {
        var h = mcc$chested();
        return h != null ? h.hasChest() : Boolean.TRUE.equals(dataManager.get(DATA_ID_CHEST));
    }

    public void setChested(boolean chested) {
        var h = mcc$chested();
        if (h != null) h.setChest(chested);
        else dataManager.set(DATA_ID_CHEST, chested);
    }

    public int getInventoryColumns() {
        return 5;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$chested() == null) tag.setBoolean("ChestedHorse", hasChest());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$chested() == null) setChested(tag.getBoolean("ChestedHorse"));
    }
}
