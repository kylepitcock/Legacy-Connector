package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 horse: status flags in watcher slot 16 (bit 2 tame, bit 4 saddled), type in slot 19, variant in slot 20. */
public class EntityHorse extends EntityAnimal {
    public EntityHorse(World world) {
        super(world);
        setSize(1.4F, 1.6F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, 0);
        dataWatcher.addObject(19, (byte) 0);
        dataWatcher.addObject(20, 0);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityHorse(mcc$world());
    }

    private boolean getHorseWatchableObjectIntAsBoolean(int bit) {
        return (dataWatcher.getWatchableObjectInt(16) & bit) != 0;
    }

    private void setHorseWatchableBoolean(int bit, boolean value) {
        int flags = dataWatcher.getWatchableObjectInt(16);
        dataWatcher.updateObject(16, value ? flags | bit : flags & ~bit);
    }

    public boolean isTame() {
        return getHorseWatchableObjectIntAsBoolean(2);
    }

    public void setHorseTamed(boolean tamed) {
        setHorseWatchableBoolean(2, tamed);
    }

    public boolean isHorseSaddled() {
        return getHorseWatchableObjectIntAsBoolean(4);
    }

    public int getHorseType() {
        return dataWatcher.getWatchableObjectByte(19);
    }

    public void setHorseType(int type) {
        dataWatcher.updateObject(19, (byte) type);
    }
}
