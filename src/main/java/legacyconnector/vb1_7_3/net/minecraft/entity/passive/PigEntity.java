package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;

/** Beta pig superclass, with saddle interaction and original cooked/raw drop selection. */
public class PigEntity extends AnimalEntity {
    public boolean saddled;
    public PigEntity(World world){super(world);texture="/mob/pig.png";setBoundingBoxSpacing(.9F,.9F);}
    public boolean isSaddled(){return saddled;}
    public void setSaddled(boolean saddled){this.saddled=saddled;}
    @Override public boolean interact(PlayerEntity player){if(saddled&&!world.isRemote&&(passenger==null||passenger==player)){player.setVehicle(this);return true;}return false;}
    @Override public void writeNbt(NbtCompound tag){super.writeNbt(tag);tag.putBoolean("Saddle",saddled);}
    @Override public void readNbt(NbtCompound tag){super.readNbt(tag);saddled=tag.getBoolean("Saddle");}
    @Override protected String getRandomSound(){return "mob.pig";}
    @Override protected String getHurtSound(){return "mob.pig";}
    @Override protected String getDeathSound(){return "mob.pigdeath";}
    @Override protected int getDroppedItemId(){return fireTicks>0?320:319;}
}
