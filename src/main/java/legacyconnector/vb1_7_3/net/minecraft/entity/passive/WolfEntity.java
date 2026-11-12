package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta animal identity for native entity views and original instanceof decisions. */
public class WolfEntity extends AnimalEntity {
    public WolfEntity(World world){super(world);}
    public boolean isTamed(){return mcc$modern instanceof net.minecraft.world.entity.TamableAnimal tamable&&tamable.isTame();}
    public void setTamed(boolean tamed){if(mcc$modern instanceof net.minecraft.world.entity.TamableAnimal tamable)tamable.setTame(tamed,true);}
    public boolean isAngry(){return mcc$modern instanceof net.minecraft.world.entity.NeutralMob angry&&angry.getRemainingPersistentAngerTime()>0;}
}
