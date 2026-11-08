package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class QuadrupedEntityModel extends EntityModel {
   public ModelPart head = new ModelPart(0, 0);
   public ModelPart body;
   public ModelPart rightHindLeg;
   public ModelPart leftHindLeg;
   public ModelPart rightFrontLeg;
   public ModelPart leftFrontLeg;

   public QuadrupedEntityModel(int var1, float var2) {
      this.head.addCuboid(-4.0F, -4.0F, -8.0F, 8, 8, 8, var2);
      this.head.setPivot(0.0F, (float)(18 - var1), -6.0F);
      this.body = new ModelPart(28, 8);
      this.body.addCuboid(-5.0F, -10.0F, -7.0F, 10, 16, 8, var2);
      this.body.setPivot(0.0F, (float)(17 - var1), 2.0F);
      this.rightHindLeg = new ModelPart(0, 16);
      this.rightHindLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, var1, 4, var2);
      this.rightHindLeg.setPivot(-3.0F, (float)(24 - var1), 7.0F);
      this.leftHindLeg = new ModelPart(0, 16);
      this.leftHindLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, var1, 4, var2);
      this.leftHindLeg.setPivot(3.0F, (float)(24 - var1), 7.0F);
      this.rightFrontLeg = new ModelPart(0, 16);
      this.rightFrontLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, var1, 4, var2);
      this.rightFrontLeg.setPivot(-3.0F, (float)(24 - var1), -5.0F);
      this.leftFrontLeg = new ModelPart(0, 16);
      this.leftFrontLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, var1, 4, var2);
      this.leftFrontLeg.setPivot(3.0F, (float)(24 - var1), -5.0F);
   }

   public void render(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.setAngles(var1, var2, var3, var4, var5, var6);
      this.head.render(var6);
      this.body.render(var6);
      this.rightHindLeg.render(var6);
      this.leftHindLeg.render(var6);
      this.rightFrontLeg.render(var6);
      this.leftFrontLeg.render(var6);
   }

   public void setAngles(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.head.pitch = var5 / (180.0F / (float)Math.PI);
      this.head.yaw = var4 / (180.0F / (float)Math.PI);
      this.body.pitch = (float) (Math.PI / 2);
      this.rightHindLeg.pitch = MathHelper.cos(var1 * 0.6662F) * 1.4F * var2;
      this.leftHindLeg.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 1.4F * var2;
      this.rightFrontLeg.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 1.4F * var2;
      this.leftFrontLeg.pitch = MathHelper.cos(var1 * 0.6662F) * 1.4F * var2;
   }
}
