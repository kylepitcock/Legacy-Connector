package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 zombie state: baby flag in watcher slot 12, villager flag in slot 13. */
public class EntityZombie extends EntityMob {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityZombie(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(12, (byte) 0);
        dataWatcher.addObject(13, (byte) 0);
        dataWatcher.addObject(14, (byte) 0);
    }

    public boolean isVillager() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted()) return z instanceof net.minecraft.world.entity.monster.ZombieVillager;
        return dataWatcher.getWatchableObjectByte(13) == 1;
    }

    public void setVillager(boolean villager) {
        dataWatcher.updateObject(13, (byte) (villager ? 1 : 0));
    }

    public boolean isChild() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted()) return z.isBaby();
        return dataWatcher.getWatchableObjectByte(12) == 1;
    }

    public void setChild(boolean child) {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted()) {
            z.setBaby(child);
            return;
        }
        dataWatcher.updateObject(12, (byte) (child ? 1 : 0));
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }
}
