package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;

/** Original outer shell or inner body and face, selected by the texture-row constructor argument. */
public class SlimeEntityModel extends EntityModel {
    ModelPart cube,rightEye,leftEye,mouth;
    public SlimeEntityModel(int textureRow){
        cube=new ModelPart(0,textureRow);
        if(textureRow<=0)cube.addCuboid(-4,16,-4,8,8,8);
        else{
            cube.addCuboid(-3,17,-3,6,6,6);
            rightEye=new ModelPart(32,0);rightEye.addCuboid(-3.25F,18,-3.5F,2,2,2);
            leftEye=new ModelPart(32,4);leftEye.addCuboid(1.25F,18,-3.5F,2,2,2);
            mouth=new ModelPart(32,8);mouth.addCuboid(0,21,-3.5F,1,1,1);
        }
    }
    @Override public void setAngles(float limb,float amount,float age,float yaw,float pitch,float scale){}
    @Override public void render(float limb,float amount,float age,float yaw,float pitch,float scale){
        setAngles(limb,amount,age,yaw,pitch,scale);cube.render(scale);
        if(rightEye!=null){rightEye.render(scale);leftEye.render(scale);mouth.render(scale);}
    }
}
