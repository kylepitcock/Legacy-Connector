package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A firework rocket in flight. The explosion effect is not drawn by the layer yet; the rocket rises and disappears. */
public class EntityFireworkRocket extends Entity {
    private int fireworkAge;
    private int lifetime;

    public EntityFireworkRocket(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityFireworkRocket(World world, double x, double y, double z, ItemStack stack) {
        this(world);
        this.lifetime = 10 * 2 + rand.nextInt(6) + rand.nextInt(7);
        setPosition(x, y, z);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        setVelocity(mcc$get$motionX(this) * 1.15D, mcc$get$motionY(this) + 0.04D, mcc$get$motionZ(this) * 1.15D);
        setPosition(mcc$get$posX(this) + mcc$get$motionX(this), mcc$get$posY(this) + mcc$get$motionY(this), mcc$get$posZ(this) + mcc$get$motionZ(this));
        if (++fireworkAge > lifetime && !mcc$world().isRemote) setDead();
    }

    @Override
    protected void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        fireworkAge = tag.getInteger("Life");
        lifetime = tag.getInteger("LifeTime");
    }

    @Override
    protected void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        tag.setInteger("Life", fireworkAge);
        tag.setInteger("LifeTime", lifetime);
    }
}
