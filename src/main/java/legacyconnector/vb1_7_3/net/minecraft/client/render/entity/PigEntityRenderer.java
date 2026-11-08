package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.passive.PigEntity;

/** Beta saddle render pass; the original model and texture callbacks remain virtual. */
public class PigEntityRenderer extends LivingEntityRenderer {
    public PigEntityRenderer(EntityModel model,EntityModel saddle,float radius){super(model,radius);setDecorationModel(saddle);}
    protected boolean bindTexture(PigEntity entity,int pass,float partial){bindTexture("/mob/saddle.png");return pass==0&&entity.isSaddled();}
    @Override protected boolean bindTexture(LivingEntity entity,int pass,float partial){return entity instanceof PigEntity pig&&bindTexture(pig,pass,partial);}
}
