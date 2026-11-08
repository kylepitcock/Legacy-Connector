package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Typed native sheep view; wool state is read and written through the native data tracker. */
public class SheepEntity extends AnimalEntity {
    private int color;
    private boolean sheared;
    public SheepEntity(World world){super(world);texture="/mob/sheep.png";setBoundingBoxSpacing(.9F,1.3F);}
    public int getColor(){return mcc$modern instanceof net.minecraft.world.entity.animal.Sheep sheep?sheep.getColor().getId():color;}
    public void setColor(int color){this.color=color&15;if(mcc$modern instanceof net.minecraft.world.entity.animal.Sheep sheep)sheep.setColor(net.minecraft.world.item.DyeColor.byId(this.color));}
    public boolean isSheared(){return mcc$modern instanceof net.minecraft.world.entity.animal.Sheep sheep?sheep.isSheared():sheared;}
    public void setSheared(boolean value){sheared=value;if(mcc$modern instanceof net.minecraft.world.entity.animal.Sheep sheep)sheep.setSheared(value);}
}
