package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 bat: the hanging flag is bit 0 of watcher slot 16. */
public class EntityBat extends EntityAmbientCreature {
    public EntityBat(World world) {
        super(world);
        setSize(0.5F, 0.9F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    public boolean getIsBatHanging() {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void setIsBatHanging(boolean hanging) {
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (hanging ? flags | 1 : flags & -2));
    }
}
