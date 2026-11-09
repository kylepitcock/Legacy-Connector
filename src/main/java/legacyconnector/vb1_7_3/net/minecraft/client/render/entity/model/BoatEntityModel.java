package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;

/** Beta's five fixed hull pieces, including the original bottom texture origin. */
public class BoatEntityModel extends EntityModel {
    public ModelPart[] parts=new ModelPart[5];
    public BoatEntityModel(){
        parts[0]=new ModelPart(0,8);
        for(int i=1;i<5;i++)parts[i]=new ModelPart(0,0);
        parts[0].addCuboid(-12,-8,-3,24,16,4,0);parts[0].setPivot(0,4,0);
        for(int i=1;i<5;i++)parts[i].addCuboid(-10,-7,-1,20,6,2,0);
        parts[1].setPivot(-11,4,0);parts[2].setPivot(11,4,0);
        parts[3].setPivot(0,4,-9);parts[4].setPivot(0,4,9);
        parts[0].pitch=(float)Math.PI/2;parts[1].yaw=(float)Math.PI*1.5F;
        parts[2].yaw=(float)Math.PI/2;parts[3].yaw=(float)Math.PI;
    }
    @Override public void render(float limb,float amount,float age,float yaw,float pitch,float scale){for(var part:parts)part.render(scale);}
}
