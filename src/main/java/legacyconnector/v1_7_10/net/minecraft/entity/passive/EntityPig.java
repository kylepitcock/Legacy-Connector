package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 pig: the saddle flag is watcher slot 16. */
public class EntityPig extends EntityAnimal {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityPig(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityPig(World world) {
        super(world);
        setSize(0.9F, 0.9F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityPig(mcc$world());
    }

    public boolean getSaddled() {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void setSaddled(boolean saddled) {
        dataWatcher.updateObject(16, (byte) (saddled ? 1 : 0));
    }
}
