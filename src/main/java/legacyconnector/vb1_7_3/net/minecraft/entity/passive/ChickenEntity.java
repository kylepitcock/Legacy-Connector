package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Original Beta chicken wing, falling and egg timer lifecycle. */
public class ChickenEntity extends AnimalEntity {
    public boolean unused;
    public float flapProgress,maxWingDeviation,prevMaxWingDeviation,prevFlapProgress,flapSpeed=1;
    public int eggLayTime;
    public ChickenEntity(World world){super(world);texture="/mob/chicken.png";setBoundingBoxSpacing(.3F,.4F);health=4;eggLayTime=random.nextInt(6000)+6000;}
    @Override public void tickMovement(){
        super.tickMovement();prevFlapProgress=flapProgress;prevMaxWingDeviation=maxWingDeviation;
        maxWingDeviation=(float)(maxWingDeviation+(onGround?-1:4)*.3);maxWingDeviation=Math.max(0,Math.min(1,maxWingDeviation));
        if(!onGround&&flapSpeed<1)flapSpeed=1;flapSpeed*=.9F;
        if(!onGround&&velocityY<0)velocityY*=.6;
        flapProgress+=flapSpeed*2;
        if(!world.isRemote&&--eggLayTime<=0){world.playSound(this,"mob.chickenplop",1,(random.nextFloat()-random.nextFloat())*.2F+1);dropItem(344,1);eggLayTime=random.nextInt(6000)+6000;}
    }
    @Override protected void onLanding(float distance){}
    @Override protected String getRandomSound(){return "mob.chicken";}
    @Override protected String getHurtSound(){return "mob.chickenhurt";}
    @Override protected String getDeathSound(){return "mob.chickenhurt";}
    @Override protected int getDroppedItemId(){return 288;}
}
