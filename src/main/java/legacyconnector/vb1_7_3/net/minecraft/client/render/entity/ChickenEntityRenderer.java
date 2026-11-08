package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.ChickenEntity;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta chicken wing-flap interpolation used by inherited vanilla renderers. */
public class ChickenEntityRenderer extends LivingEntityRenderer {
    public ChickenEntityRenderer(EntityModel model,float radius){super(model,radius);}
    public void render(ChickenEntity entity,double x,double y,double z,float yaw,float partial){super.render(entity,x,y,z,yaw,partial);}
    @Override public void render(LivingEntity entity,double x,double y,double z,float yaw,float partial){if(entity instanceof ChickenEntity chicken)render(chicken,x,y,z,yaw,partial);else super.render(entity,x,y,z,yaw,partial);}
    protected float getHeadBob(ChickenEntity entity,float partial){
        float flap=entity.prevFlapProgress+(entity.flapProgress-entity.prevFlapProgress)*partial;
        float deviation=entity.prevMaxWingDeviation+(entity.maxWingDeviation-entity.prevMaxWingDeviation)*partial;
        return (MathHelper.sin(flap)+1)*deviation;
    }
    @Override protected float getHeadBob(LivingEntity entity,float partial){return entity instanceof ChickenEntity chicken?getHeadBob(chicken,partial):super.getHeadBob(entity,partial);}
}
