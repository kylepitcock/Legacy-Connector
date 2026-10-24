package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 skeleton: the type (0 skeleton, 1 wither skeleton) lives in watcher slot 13. */
public class EntitySkeleton extends EntityMob {
    public EntitySkeleton(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(13, (byte) 0);
    }

    public int getSkeletonType() {
        return dataWatcher.getWatchableObjectByte(13);
    }

    public void setSkeletonType(int type) {
        dataWatcher.updateObject(13, (byte) type);
        isImmuneToFire = type == 1;
        if (type == 1) setSize(0.72F, 2.34F);
        else setSize(0.6F, 1.8F);
    }
}
