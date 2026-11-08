package legacyconnector.vb1_7_3.net.minecraft.entity.projectile;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Typed arrow identity and shooter field used by original combat callbacks. */
public class ArrowEntity extends Entity {
    public LivingEntity owner;
    public ArrowEntity(World world){super(world);setBoundingBoxSpacing(.5F,.5F);}
    public ArrowEntity(World world,LivingEntity owner){
        this(world);this.owner=owner;
        setPositionAndAngles(owner.x,owner.y+owner.getEyeHeight(),owner.z,owner.yaw,owner.pitch);
        double yaw=Math.toRadians(this.yaw),pitch=Math.toRadians(this.pitch);
        setPosition(x-Math.cos(yaw)*.16,y-.1,z-Math.sin(yaw)*.16);
        setArrowHeading(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch),1.5F,1);
    }
    public ArrowEntity(World world,double x,double y,double z){this(world);setPosition(x,y,z);}
    public void setVelocity(double x,double y,double z,float speed,float inaccuracy){setArrowHeading(x,y,z,speed,inaccuracy);}
    public void setArrowHeading(double x,double y,double z,float speed,float inaccuracy){
        double length=Math.sqrt(x*x+y*y+z*z);if(length==0)return;
        velocityX=(x/length+random.nextGaussian()*.0075*inaccuracy)*speed;
        velocityY=(y/length+random.nextGaussian()*.0075*inaccuracy)*speed;
        velocityZ=(z/length+random.nextGaussian()*.0075*inaccuracy)*speed;
        yaw=prevYaw=(float)(Math.atan2(velocityX,velocityZ)*180/Math.PI);
        pitch=prevPitch=(float)(Math.atan2(velocityY,Math.sqrt(velocityX*velocityX+velocityZ*velocityZ))*180/Math.PI);
    }
}
