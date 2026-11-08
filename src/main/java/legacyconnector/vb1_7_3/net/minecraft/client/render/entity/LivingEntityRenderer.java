package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;

/** Original Beta living renderer callback order, using the supplied original model and virtual renderer hooks. */
public class LivingEntityRenderer extends EntityRenderer {
    protected EntityModel model;
    protected EntityModel decorationModel;
    public LivingEntityRenderer(EntityModel model,float radius){this.model=model;shadowRadius=radius;}
    public void setDecorationModel(EntityModel model){decorationModel=model;}
    public final EntityModel mcc$model(){return model;}
    public final EntityModel mcc$decorationModel(){return decorationModel;}
    @Override public void render(Entity entity,double x,double y,double z,float yaw,float partial){render((LivingEntity)entity,x,y,z,yaw,partial);}
    public void render(LivingEntity entity,double x,double y,double z,float yaw,float partial){
        if(model==null)throw new IllegalStateException("Beta living renderer has no model");
        GL11.glPushMatrix();
        try{
            model.handSwingProgress=getHandSwingProgress(entity,partial);model.riding=entity.vehicle!=null;
            if(decorationModel!=null){decorationModel.handSwingProgress=model.handSwingProgress;decorationModel.riding=model.riding;}
            float body=entity.lastBodyYaw+(entity.bodyYaw-entity.lastBodyYaw)*partial;
            float head=entity.prevYaw+(entity.yaw-entity.prevYaw)*partial;
            float pitch=entity.prevPitch+(entity.pitch-entity.prevPitch)*partial;
            applyTranslation(entity,x,y,z);
            float age=getHeadBob(entity,partial);
            applyHandSwingRotation(entity,age,body,partial);
            GL11.glScalef(-1,-1,1);
            applyScale(entity,partial);
            GL11.glTranslatef(0,-1.5078125F,0);
            float amount=Math.min(1,entity.lastWalkAnimationSpeed+(entity.walkAnimationSpeed-entity.lastWalkAnimationSpeed)*partial);
            float limb=entity.walkAnimationProgress-entity.walkAnimationSpeed*(1-partial);
            bindDownloadedTexture(entity.skinUrl,entity.mcc$texture());
            model.animateModel(entity,limb,amount,partial);
            model.render(limb,amount,age,head-body,pitch,.0625F);
            for(int pass=0;pass<4;pass++)if(bindDecorationTexture(entity,pass,partial)&&decorationModel!=null)decorationModel.render(limb,amount,age,head-body,pitch,.0625F);
            renderMore(entity,partial);
        }finally{GL11.glPopMatrix();}
        renderNameTag(entity,x,y,z);
    }
    protected void applyTranslation(LivingEntity entity,double x,double y,double z){GL11.glTranslated(x,y,z);}
    protected void applyHandSwingRotation(LivingEntity entity,float age,float bodyYaw,float partial){
        GL11.glRotatef(180-bodyYaw,0,1,0);
        if(entity.deathTime>0){float progress=(float)Math.sqrt(Math.max(0,(entity.deathTime+partial-1)/20F*1.6F));GL11.glRotatef(Math.min(progress,1)*getDeathYaw(entity),0,0,1);}
    }
    protected float getHandSwingProgress(LivingEntity entity,float partial){return entity.mcc$modern instanceof net.minecraft.world.entity.LivingEntity nativeLiving?nativeLiving.getAttackAnim(partial):0;}
    protected float getHeadBob(LivingEntity entity,float partial){return entity.age+partial;}
    protected void renderMore(LivingEntity entity,float partial){}
    protected boolean bindDecorationTexture(LivingEntity entity,int pass,float partial){return bindTexture(entity,pass,partial);}
    protected boolean bindTexture(LivingEntity entity,int pass,float partial){return false;}
    protected float getDeathYaw(LivingEntity entity){return 90;}
    protected int getOverlayColor(LivingEntity entity,float brightness,float partial){return 0;}
    protected void applyScale(LivingEntity entity,float partial){}
    protected void renderNameTag(LivingEntity entity,double x,double y,double z){}
    protected void renderNameTag(LivingEntity entity,String name,double x,double y,double z,int distance){}
}
