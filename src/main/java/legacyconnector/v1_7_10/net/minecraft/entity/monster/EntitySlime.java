package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 slime: size in watcher slot 16. */
public class EntitySlime extends EntityLiving implements IMob {
    public EntitySlime(World world) {
        super(world);
        setSlimeSize(1);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 1);
    }

    public int getSlimeSize() {
        return dataWatcher.getWatchableObjectByte(16);
    }

    public void setSlimeSize(int size) {
        dataWatcher.updateObject(16, (byte) size);
        setSize(0.6F * size, 0.6F * size);
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
    }
}
