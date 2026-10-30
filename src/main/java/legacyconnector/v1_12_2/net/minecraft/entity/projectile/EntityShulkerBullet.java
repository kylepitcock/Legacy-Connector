package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 shulker bullet (the modern homing bullet). */
public class EntityShulkerBullet extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityShulkerBullet(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
    }

    /** A bullet fired by {@code owner} that homes in on {@code target}, setting off along {@code axis} (the modern bullet). */
    public EntityShulkerBullet(World world, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase owner,
            legacyconnector.v1_12_2.net.minecraft.entity.Entity target, legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.Axis axis) {
        this(world);
        var m = mcc$self();
        var o = owner == null ? null : owner.mcc$living();
        var t = target == null ? null : target.mcc$modern();
        if (m != null && o != null && t != null) {
            var a = axis == null ? net.minecraft.core.Direction.Axis.Y : net.minecraft.core.Direction.Axis.valueOf(axis.name());
            org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.bind(this, new net.minecraft.world.entity.projectile.ShulkerBullet(m.level(), o, t, a));
        } else if (owner != null) {
            var pos = owner.getPosition();
            setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, mcc$get$rotationYaw(this), mcc$get$rotationPitch(this));
        }
    }

    protected EntityShulkerBullet(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.projectile.ShulkerBullet mcc$self() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.projectile.ShulkerBullet m ? m : null;
    }

    @Override
    protected void entityInit() {
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
