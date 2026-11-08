package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta flying superclass: original friction and fluid movement, without gravity or landing damage. */
public abstract class FlyingEntity extends LivingEntity {
    public FlyingEntity(World world){super(world);}
    @Override protected void onLanding(float distance){}
    @Override public boolean isOnLadder(){return false;}
    @Override public void travel(float sideways,float forward){
        if(checkWaterCollisions()){moveNonSolid(sideways,forward,.02F);move(velocityX,velocityY,velocityZ);velocityX*=.8;velocityY*=.8;velocityZ*=.8;}
        else if(isTouchingLava()){moveNonSolid(sideways,forward,.02F);move(velocityX,velocityY,velocityZ);velocityX*=.5;velocityY*=.5;velocityZ*=.5;}
        else{
            float friction=friction();float acceleration=.16277136F/(friction*friction*friction);
            moveNonSolid(sideways,forward,onGround?.1F*acceleration:.02F);
            friction=friction();move(velocityX,velocityY,velocityZ);velocityX*=friction;velocityY*=friction;velocityZ*=friction;
        }
        lastWalkAnimationSpeed=walkAnimationSpeed;
        double dx=x-prevX,dz=z-prevZ;float amount=Math.min(1,(float)Math.sqrt(dx*dx+dz*dz)*4);
        walkAnimationSpeed+=(amount-walkAnimationSpeed)*.4F;walkAnimationProgress+=walkAnimationSpeed;
    }
    private float friction(){
        if(!onGround)return .91F;
        if(mcc$modern==null||world==null||world.mcc$level()==null)return .54600006F;
        var pos=net.minecraft.core.BlockPos.containing(x,boundingBox.minY-1,z);var level=world.mcc$level();
        return level.getBlockState(pos).getFriction(level,pos,mcc$modern)*.91F;
    }
}
