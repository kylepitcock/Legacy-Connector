package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 spider: the climbing flag is bit 0 of watcher slot 16. */
public class EntitySpider extends EntityMob {
    public EntitySpider(World world) {
        super(world);
        setSize(1.4F, 0.9F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    public boolean isBesideClimbableBlock() {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (climbing ? flags | 1 : flags & -2));
    }
}
