package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class CowEntityModel extends QuadrupedEntityModel {
   ModelPart udder;
   ModelPart rightHorn;
   ModelPart leftHorn;

   public CowEntityModel() {
      super(12, 0.0F);
      this.head = new ModelPart(0, 0);
      this.head.addCuboid(-4.0F, -4.0F, -6.0F, 8, 8, 6, 0.0F);
      this.head.setPivot(0.0F, 4.0F, -8.0F);
      this.rightHorn = new ModelPart(22, 0);
      this.rightHorn.addCuboid(-4.0F, -5.0F, -4.0F, 1, 3, 1, 0.0F);
      this.rightHorn.setPivot(0.0F, 3.0F, -7.0F);
      this.leftHorn = new ModelPart(22, 0);
      this.leftHorn.addCuboid(3.0F, -5.0F, -4.0F, 1, 3, 1, 0.0F);
      this.leftHorn.setPivot(0.0F, 3.0F, -7.0F);
      this.udder = new ModelPart(52, 0);
      this.udder.addCuboid(-2.0F, -3.0F, 0.0F, 4, 6, 2, 0.0F);
      this.udder.setPivot(0.0F, 14.0F, 6.0F);
      this.udder.pitch = (float) (Math.PI / 2);
      this.body = new ModelPart(18, 4);
      this.body.addCuboid(-6.0F, -10.0F, -7.0F, 12, 18, 10, 0.0F);
      this.body.setPivot(0.0F, 5.0F, 2.0F);
      this.rightHindLeg.pivotX--;
      this.leftHindLeg.pivotX++;
      this.rightHindLeg.pivotZ += 0.0F;
      this.leftHindLeg.pivotZ += 0.0F;
      this.rightFrontLeg.pivotX--;
      this.leftFrontLeg.pivotX++;
      this.rightFrontLeg.pivotZ--;
      this.leftFrontLeg.pivotZ--;
   }

   @Override
   public void render(float var1, float var2, float var3, float var4, float var5, float var6) {
      super.render(var1, var2, var3, var4, var5, var6);
      this.rightHorn.render(var6);
      this.leftHorn.render(var6);
      this.udder.render(var6);
   }

   @Override
   public void setAngles(float var1, float var2, float var3, float var4, float var5, float var6) {
      super.setAngles(var1, var2, var3, var4, var5, var6);
      this.rightHorn.yaw = this.head.yaw;
      this.rightHorn.pitch = this.head.pitch;
      this.leftHorn.yaw = this.head.yaw;
      this.leftHorn.pitch = this.head.pitch;
   }
}
