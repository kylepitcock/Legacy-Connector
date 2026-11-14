package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.SheepEntity;

/** Beta sheep wool render pass: the fur model, tinted by the fleece colour, while the sheep is not sheared. */
public class SheepEntityRenderer extends LivingEntityRenderer {
    public SheepEntityRenderer(EntityModel model,EntityModel wool,float radius){super(model,radius);setDecorationModel(wool);}
    protected boolean bindTexture(SheepEntity entity,int pass,float partial){
        if(pass!=0||entity.isSheared())return false;
        bindTexture("/mob/sheep_fur.png");
        float[] c=SheepEntity.COLORS[entity.getColor()&15];
        legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11.glColor3f(c[0],c[1],c[2]);
        return true;
    }
    @Override protected boolean bindTexture(LivingEntity entity,int pass,float partial){return entity instanceof SheepEntity sheep&&bindTexture(sheep,pass,partial);}
}
