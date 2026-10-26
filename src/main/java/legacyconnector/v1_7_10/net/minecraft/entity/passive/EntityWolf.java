package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 wolf: angry is bit 2 of watcher slot 16, collar colour is slot 20. */
public class EntityWolf extends EntityTameable {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityWolf(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityWolf(World world) {
        super(world);
        setSize(0.6F, 0.8F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(18, 0.0F);
        dataWatcher.addObject(19, (byte) 0);
        dataWatcher.addObject(20, (byte) 1);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityWolf(mcc$world());
    }

    public boolean isAngry() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) return w.isAngry();
        return (dataWatcher.getWatchableObjectByte(16) & 2) != 0;
    }

    public void setAngry(boolean angry) {
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (angry ? flags | 2 : flags & -3));
    }

    public int getCollarColor() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) return w.getCollarColor().getId();
        return dataWatcher.getWatchableObjectByte(20) & 15;
    }

    public void setCollarColor(int color) {
        dataWatcher.updateObject(20, (byte) (color & 15));
    }

    /** Begging: tilts its head at a player holding food (watcher slot 19; a modern wolf's own "interested" flag). */
    public void func_70918_i(boolean begging) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) w.setIsInterested(begging);
        else dataWatcher.updateObject(19, (byte) (begging ? 1 : 0));
    }

    public boolean func_70922_bv() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) return w.isInterested();
        return dataWatcher.getWatchableObjectByte(19) == 1;
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }

    /** The head tilt (radians) of a wolf begging for food. */
    public float getInterestedAngle(float partialTicks) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) return w.getHeadRollAngle(partialTicks);
        return 0.0F;
    }

    /** The roll (radians) of a body part while the wolf shakes itself dry; {@code offset} delays the part. */
    public float getShakeAngle(float partialTicks, float offset) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) return w.getBodyRollAngle(partialTicks, offset);
        return 0.0F;
    }

    /** How dark the wet fur is drawn (1 dry). */
    public float getShadingWhileShaking(float partialTicks) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Wolf w && !mcc$hosted()) return w.getWetShade(partialTicks);
        return 1.0F;
    }
}
