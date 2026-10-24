package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Lit TNT: counts {@link #fuse} down from 80 ticks and explodes. Mods subclass it for their own explosives. */
public class EntityTNTPrimed extends Entity {
    public int fuse = 80;
    private EntityLivingBase tntPlacedBy;

    public EntityTNTPrimed(World world) {
        super(world);
        setSize(0.98F, 0.98F);
    }

    public EntityTNTPrimed(World world, double x, double y, double z, EntityLivingBase placedBy) {
        this(world);
        setPosition(x, y, z);
        this.tntPlacedBy = placedBy;
    }

    public EntityLivingBase getTntPlacedBy() {
        return tntPlacedBy;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (--fuse <= 0) {
            setDead();
            if (!mcc$world().isRemote) explode();
        }
    }

    private void explode() {
        mcc$world().createExplosion(this, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), 4.0F, true);
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        fuse = tag.getByte("Fuse");
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        tag.setByte("Fuse", (byte) fuse);
    }
}
