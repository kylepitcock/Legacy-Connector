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

/** 1.12.2 shulker: sits on a block face and shoots homing bullets (a view reads the modern shulker). */
public class EntityShulker extends EntityGolem implements IMob {
    public EntityShulker(World world) {
        super(world);
        setSize(1.0F, 1.0F);
    }

    protected EntityShulker(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.monster.Shulker mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Shulker m ? m : null;
    }

    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos mcc$attached;

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getAttachmentPos() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(m.blockPosition()) : mcc$attached;
    }

    public void setAttachmentPos(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        mcc$attached = pos;
        if (pos != null) setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumFacing getAttachmentFacing() {
        var m = mcc$self();
        return m != null && !mcc$hosted() ? legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(m.getAttachFace()) : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.DOWN;
    }

    public legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor getColor() {
        var m = mcc$self();
        var c = m == null ? null : m.getColor();
        return c == null ? legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.PURPLE : legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(c.getId());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.SHULKER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.SHULKER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.SHULKER_DEATH);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/shulker");
    }
}
