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

/** 1.12.2 illager base (vindicators, evokers, illusioners): arm poses and aggression shown to clients. */
public abstract class AbstractIllager extends EntityMob {
    protected static final DataParameter<Byte> AGGRESSIVE = EntityDataManager.createKey(AbstractIllager.class, DataSerializers.BYTE);

    public AbstractIllager(World world) {
        super(world);
    }

    protected AbstractIllager(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.AbstractIllager mcc$illager() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.monster.AbstractIllager i ? i : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(AGGRESSIVE, (byte) 0);
    }

    protected boolean isAggressive(int mask) {
        var i = mcc$illager();
        if (i != null) return i.isAggressive();
        Byte b = dataManager.get(AGGRESSIVE);
        return b != null && (b & mask) != 0;
    }

    protected void setAggressive(int mask, boolean value) {
        var i = mcc$illager();
        if (i != null) {
            i.setAggressive(value);
            return;
        }
        Byte b = dataManager.get(AGGRESSIVE);
        byte v = b == null ? 0 : b;
        dataManager.set(AGGRESSIVE, (byte) (value ? v | mask : v & ~mask));
    }

    public IllagerArmPose getArmPose() {
        var i = mcc$illager();
        if (i != null) {
            return switch (i.getArmPose()) {
                case ATTACKING -> IllagerArmPose.ATTACKING;
                case SPELLCASTING -> IllagerArmPose.SPELLCASTING;
                case BOW_AND_ARROW -> IllagerArmPose.BOW_AND_ARROW;
                default -> IllagerArmPose.CROSSED;
            };
        }
        return IllagerArmPose.CROSSED;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute getCreatureAttribute() {
        return legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute.ILLAGER;
    }

    /** How an illager holds its arms (the renderer picks the model pose from it). */
    public enum IllagerArmPose {
        CROSSED, ATTACKING, SPELLCASTING, BOW_AND_ARROW
    }
}
