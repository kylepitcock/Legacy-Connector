package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 blaze: the on-fire flag is bit 0 of watcher slot 16. */
public class EntityBlaze extends EntityMob {
    public EntityBlaze(World world) {
        super(world);
        this.isImmuneToFire = true;
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    public boolean func_70845_n() {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void func_70844_a(boolean onFire) {
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (onFire ? flags | 1 : flags & -2));
    }
}
