package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.BipedEntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerRenderPhasesB173;

/** Beta player callback overloads. Native player rendering owns the base skin, armor and held items. */
public class PlayerEntityRenderer extends LivingEntityRenderer {
    private BipedEntityModel bipedModel;
    private BipedEntityModel armor1=new BipedEntityModel(1F),armor2=new BipedEntityModel(.5F);
    private static final String[] armorTextureNames={"cloth","chain","iron","diamond","gold"};
    public PlayerEntityRenderer(){super(new BipedEntityModel(0),.5F);bipedModel=(BipedEntityModel)model;}
    @Override public void render(Entity entity,double x,double y,double z,float yaw,float partial){render((PlayerEntity)entity,x,y,z,yaw,partial);}
    @Override public void render(LivingEntity entity,double x,double y,double z,float yaw,float partial){render((PlayerEntity)entity,x,y,z,yaw,partial);}
    public void render(PlayerEntity entity,double x,double y,double z,float yaw,float partial){
        armor1.rightArmPose=armor2.rightArmPose=bipedModel.rightArmPose=entity.inventory.getSelectedItem()!=null;
        armor1.sneaking=armor2.sneaking=bipedModel.sneaking=entity.isSneaking();
        double feetY=y-entity.standingEyeHeight;
        if(entity.isSneaking()&&!(entity instanceof ClientPlayerEntity))feetY-=.125;
        try{
            if(!PlayerRenderPhasesB173.nativeBase()){super.render(entity,x,feetY,z,yaw,partial);return;}
            bindTexture("/mob/char.png");
            // The native base is drawn once between Pre/Post. Original renderMore still runs in
            // Beta's model coordinate system, not the already-popped native model pose.
            GL11.glPushMatrix();
            try{
                bipedModel.handSwingProgress=getHandSwingProgress(entity,partial);bipedModel.riding=entity.vehicle!=null;
                float body=entity.lastBodyYaw+(entity.bodyYaw-entity.lastBodyYaw)*partial;
                float head=entity.prevYaw+(entity.yaw-entity.prevYaw)*partial;
                float pitch=entity.prevPitch+(entity.pitch-entity.prevPitch)*partial;
                applyTranslation(entity,x,feetY,z);
                float age=getHeadBob(entity,partial);applyHandSwingRotation(entity,age,body,partial);
                GL11.glScalef(-1,-1,1);applyScale(entity,partial);GL11.glTranslatef(0,-1.5078125F,0);
                float amount=Math.min(1,entity.lastWalkAnimationSpeed+(entity.walkAnimationSpeed-entity.lastWalkAnimationSpeed)*partial);
                float limb=entity.walkAnimationProgress-entity.walkAnimationSpeed*(1-partial);
                bipedModel.animateModel(entity,limb,amount,partial);bipedModel.setAngles(limb,amount,age,head-body,pitch,.0625F);
                renderMore((LivingEntity)entity,partial);
            }finally{GL11.glPopMatrix();}
        }finally{
            armor1.sneaking=armor2.sneaking=bipedModel.sneaking=false;
            armor1.rightArmPose=armor2.rightArmPose=bipedModel.rightArmPose=false;
        }
    }
    public void renderHand(){
        if(PlayerRenderPhasesB173.nativeBase())return;
        bipedModel.handSwingProgress=0;bipedModel.setAngles(0,0,0,0,0,.0625F);bipedModel.rightArm.render(.0625F);
    }
    /** The native base already includes standard head/held-item/cape layers. */
    protected void renderMore(PlayerEntity entity,float partial){}
    @Override protected void renderMore(LivingEntity entity,float partial){renderMore((PlayerEntity)entity,partial);}
    protected void applyScale(PlayerEntity entity,float partial){GL11.glScalef(.9375F,.9375F,.9375F);}
    @Override protected void applyScale(LivingEntity entity,float partial){applyScale((PlayerEntity)entity,partial);}
    protected boolean bindTexture(PlayerEntity entity,int pass,float partial){
        var stack=entity.inventory.getArmorStack(3-pass);
        if(stack==null||!(stack.getItem() instanceof legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem armor))return false;
        bindTexture("/armor/"+armorTextureNames[armor.textureIndex]+"_"+(pass==2?2:1)+".png");
        var selected=pass==2?armor2:armor1;
        selected.head.visible=selected.hat.visible=pass==0;selected.body.visible=pass==1||pass==2;
        selected.rightArm.visible=selected.leftArm.visible=pass==1;
        selected.rightLeg.visible=selected.leftLeg.visible=pass==2||pass==3;setDecorationModel(selected);return true;
    }
    @Override protected boolean bindTexture(LivingEntity entity,int pass,float partial){return bindTexture((PlayerEntity)entity,pass,partial);}
    protected void applyTranslation(PlayerEntity entity,double x,double y,double z){
        if(entity.isAlive()&&entity.isSleeping())super.applyTranslation(entity,x+entity.sleepOffsetX,y+entity.sleepOffsetY,z+entity.sleepOffsetZ);
        else super.applyTranslation(entity,x,y,z);
    }
    @Override protected void applyTranslation(LivingEntity entity,double x,double y,double z){applyTranslation((PlayerEntity)entity,x,y,z);}
    protected void applyHandSwingRotation(PlayerEntity entity,float age,float bodyYaw,float partial){
        if(entity.isAlive()&&entity.isSleeping()){
            GL11.glRotatef(entity.getSleepingRotation(),0,1,0);GL11.glRotatef(getDeathYaw(entity),0,0,1);GL11.glRotatef(270,0,1,0);
        }else super.applyHandSwingRotation(entity,age,bodyYaw,partial);
    }
    @Override protected void applyHandSwingRotation(LivingEntity entity,float age,float bodyYaw,float partial){applyHandSwingRotation((PlayerEntity)entity,age,bodyYaw,partial);}
}
