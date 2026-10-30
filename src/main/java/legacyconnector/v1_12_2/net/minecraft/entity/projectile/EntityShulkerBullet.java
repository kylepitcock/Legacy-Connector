package legacyconnector.v1_12_2.net.minecraft.entity.projectile;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 shulker bullet (the modern homing bullet). */
public class EntityShulkerBullet extends legacyconnector.v1_12_2.net.minecraft.entity.Entity {
    public EntityShulkerBullet(World world) {
        super(world);
        setSize(0.3125F, 0.3125F);
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
