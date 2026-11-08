package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class ZombieEntityModel extends BipedEntityModel {
   @Override
   public void setAngles(float var1, float var2, float var3, float var4, float var5, float var6) {
      super.setAngles(var1, var2, var3, var4, var5, var6);
      float var7 = MathHelper.sin(this.handSwingProgress * (float) Math.PI);
      float var8 = MathHelper.sin((1.0F - (1.0F - this.handSwingProgress) * (1.0F - this.handSwingProgress)) * (float) Math.PI);
      this.rightArm.roll = 0.0F;
      this.leftArm.roll = 0.0F;
      this.rightArm.yaw = -(0.1F - var7 * 0.6F);
      this.leftArm.yaw = 0.1F - var7 * 0.6F;
      this.rightArm.pitch = (float) (-Math.PI / 2);
      this.leftArm.pitch = (float) (-Math.PI / 2);
      this.rightArm.pitch -= var7 * 1.2F - var8 * 0.4F;
      this.leftArm.pitch -= var7 * 1.2F - var8 * 0.4F;
      this.rightArm.roll = this.rightArm.roll + MathHelper.cos(var3 * 0.09F) * 0.05F + 0.05F;
      this.leftArm.roll = this.leftArm.roll - (MathHelper.cos(var3 * 0.09F) * 0.05F + 0.05F);
      this.rightArm.pitch = this.rightArm.pitch + MathHelper.sin(var3 * 0.067F) * 0.05F;
      this.leftArm.pitch = this.leftArm.pitch - MathHelper.sin(var3 * 0.067F) * 0.05F;
   }
}
