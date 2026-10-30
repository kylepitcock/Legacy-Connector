package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 end crystal: beam target and base plate over the modern crystal. */
public class EntityEnderCrystal extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityEnderCrystal(World world) {
        super(world);
        setSize(2.0F, 2.0F);
    }

    public EntityEnderCrystal(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    protected EntityEnderCrystal(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.boss.enderdragon.EndCrystal mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal m ? m : null;
    }

    @Override
    protected void entityInit() {
    }

    private legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos mcc$beam;
    private boolean mcc$bottom = true;

    public void setBeamTarget(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var m = mcc$self();
        if (m != null) m.setBeamTarget(pos == null ? null : pos.mcc$modern());
        mcc$beam = pos;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos getBeamTarget() {
        var m = mcc$self();
        if (m != null) return m.getBeamTarget() == null ? null : legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(m.getBeamTarget());
        return mcc$beam;
    }

    public void setShowBottom(boolean show) {
        var m = mcc$self();
        if (m != null) m.setShowBottom(show);
        mcc$bottom = show;
    }

    public boolean shouldShowBottom() {
        var m = mcc$self();
        return m != null ? m.showsBottom() : mcc$bottom;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
