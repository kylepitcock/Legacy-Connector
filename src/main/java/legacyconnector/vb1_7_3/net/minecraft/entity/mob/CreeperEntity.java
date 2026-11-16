package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Typed native creeper view; legacy callbacks may reset the live fuse timer. Mods' creepers keep Beta's own fuse fields. */
public class CreeperEntity extends MonsterEntity {
    public int fuseTime;
    public int lastFuseTime;
    private boolean mcc$charged;
    private int mcc$fuseSpeed=-1;
    public CreeperEntity(World world){super(world);texture="/mob/creeper.png";}

    /** Beta's powered (lightning-struck) creeper; a native creeper answers from its own state. */
    public boolean isCharged(){return mcc$modern instanceof net.minecraft.world.entity.monster.Creeper creeper?creeper.isPowered():mcc$charged;}
    public void mcc$setCharged(boolean charged){mcc$charged=charged;}
    /** Beta's fuse direction: -1 idle, 1 burning. */
    public int getFuseSpeed(){return mcc$modern instanceof net.minecraft.world.entity.monster.Creeper creeper?creeper.getSwellDir():mcc$fuseSpeed;}
    public void setFuseSpeed(int speed){if(mcc$modern instanceof net.minecraft.world.entity.monster.Creeper creeper)creeper.setSwellDir(speed);else mcc$fuseSpeed=speed;}

    /** Beta's {@code setCreeperFlashTime}: how far the fuse has burnt (0..1), between ticks; a native creeper's own swelling. */
    public float getScale(float partial) {
        if (mcc$modern instanceof net.minecraft.world.entity.monster.Creeper creeper) return creeper.getSwelling(partial);
        return (lastFuseTime + (fuseTime - lastFuseTime) * partial) / 28.0F;
    }
}
