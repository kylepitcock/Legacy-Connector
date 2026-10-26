package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 slime: size in watcher slot 16. */
public class EntitySlime extends EntityLiving implements IMob {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntitySlime(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

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
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Slime s && !mcc$hosted()) return s.getSize();
        return dataWatcher.getWatchableObjectByte(16);
    }

    public void setSlimeSize(int size) {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Slime s && !mcc$hosted()) {
            s.setSize(size, true);
            return;
        }
        dataWatcher.updateObject(16, (byte) size);
        setSize(0.6F * size, 0.6F * size);
        setPosition(mcc$get$posX(this), mcc$get$posY(this), mcc$get$posZ(this));
    }
}
