package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta animal identity for native entity views and original instanceof decisions. A native wolf answers from its own state; a mod's wolf
 * (Twilight Forest's hostile wolf) keeps Beta's anger, sitting, taming and owner, hunts players while angry and bites as Beta's wolf did.
 */
public class WolfEntity extends AnimalEntity {
    private boolean mcc$angry, mcc$sitting, mcc$tamed;
    private String mcc$ownerName = "";

    public WolfEntity(World world){super(world);}

    private boolean nativeWolf(){return mcc$modern instanceof net.minecraft.world.entity.animal.Wolf;}

    public boolean isTamed(){return nativeWolf()?((net.minecraft.world.entity.TamableAnimal)mcc$modern).isTame():mcc$tamed;}
    public void setTamed(boolean tamed){if(nativeWolf())((net.minecraft.world.entity.TamableAnimal)mcc$modern).setTame(tamed,true);else mcc$tamed=tamed;}
    public boolean isAngry(){return nativeWolf()?((net.minecraft.world.entity.NeutralMob)mcc$modern).getRemainingPersistentAngerTime()>0:mcc$angry;}
    public void setAngry(boolean angry){
        if(nativeWolf()){var wolf=(net.minecraft.world.entity.NeutralMob)mcc$modern;if(angry)wolf.startPersistentAngerTimer();else wolf.stopBeingAngry();}
        else mcc$angry=angry;
    }
    public boolean isInSittingPose(){return nativeWolf()?((net.minecraft.world.entity.TamableAnimal)mcc$modern).isInSittingPose():mcc$sitting;}
    public void setSitting(boolean sitting){if(nativeWolf())((net.minecraft.world.entity.TamableAnimal)mcc$modern).setInSittingPose(sitting);else mcc$sitting=sitting;}
    /** Beta's tail angle: raised when angry, lowered with lost health when tamed. */
    public float getTailAngle(){if(isAngry())return 1.5393804F;if(isTamed())return (0.55F-(20-health)*0.02F)*(float)Math.PI;return (float)Math.PI/5.0F;}
    public String getOwnerName(){return mcc$ownerName;}
    public void setOwnerName(String name){mcc$ownerName=name==null?"":name;}

    /** Beta's wolf: an angry one hunts the closest player. */
    @Override protected Entity getTargetInRange(){
        return isAngry()&&world!=null?org.kylepitcock.legacyconnector.vb1_7_3.CombatTargetsB173.closest(world,this,16,"wolf target"):null;
    }

    /** Beta's wolf: leaps from two to six blocks, bites (2, tamed 4) closer than one and a half. */
    @Override protected void attack(Entity victim,float distance){
        if(distance>2.0F&&distance<6.0F&&random.nextInt(10)==0){
            if(onGround){double dx=victim.x-x,dz=victim.z-z;double length=Math.max(1.0E-4,Math.sqrt(dx*dx+dz*dz));
                velocityX=dx/length*0.5D*0.8D+velocityX*0.2D;velocityZ=dz/length*0.5D*0.8D+velocityZ*0.2D;velocityY=0.4D;}
        }else if(distance<1.5D&&victim.boundingBox.maxY>boundingBox.minY&&victim.boundingBox.minY<boundingBox.maxY){
            attackCooldown=20;victim.damage(this,isTamed()?4:2);
        }
    }
}
