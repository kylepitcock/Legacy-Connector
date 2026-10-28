package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 eye of ender: flies toward a target (the modern eye). */
public class EntityEnderEye extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityEnderEye(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityEnderEye(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
        var m = mcc$self();
        if (m != null) m.setItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_EYE));
    }

    protected EntityEnderEye(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.projectile.EyeOfEnder mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.EyeOfEnder m ? m : null;
    }

    @Override
    protected void entityInit() {
    }

    public void moveTowards(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        var m = mcc$self();
        if (m != null && pos != null) m.signalTo(pos.mcc$modern());
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }
}
