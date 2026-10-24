package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 creeper: fuse state in watcher slot 16 (-1 idle, 1 lit), charged flag in slot 17. */
public class EntityCreeper extends EntityMob {
    public EntityCreeper(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) -1);
        dataWatcher.addObject(17, (byte) 0);
    }

    public boolean getPowered() {
        return dataWatcher.getWatchableObjectByte(17) == 1;
    }

    public int getCreeperState() {
        return dataWatcher.getWatchableObjectByte(16);
    }

    public void setCreeperState(int state) {
        dataWatcher.updateObject(16, (byte) state);
    }

    /** Blows the creeper up (private in 1.7.10 but named in mods that override it). */
    protected void func_146077_cc() {
        if (mcc$world() != null && !mcc$world().isRemote) {
            float power = getPowered() ? 2.0F : 1.0F;
            mcc$world().createExplosion(this, mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this), 3.0F * power, true);
            setDead();
        }
    }
}
