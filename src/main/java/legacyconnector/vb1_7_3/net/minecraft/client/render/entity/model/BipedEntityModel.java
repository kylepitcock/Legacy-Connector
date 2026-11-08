package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class BipedEntityModel extends EntityModel {
   public ModelPart head;
   public ModelPart hat;
   public ModelPart body;
   public ModelPart rightArm;
   public ModelPart leftArm;
   public ModelPart rightLeg;
   public ModelPart leftLeg;
   public ModelPart ears;
   public ModelPart cape;
   public boolean leftArmPose = false;
   public boolean rightArmPose = false;
   public boolean sneaking = false;

   public BipedEntityModel() {
      this(0.0F);
   }

   public BipedEntityModel(float var1) {
      this(var1, 0.0F);
   }

   public BipedEntityModel(float var1, float var2) {
      this.cape = new ModelPart(0, 0);
      this.cape.addCuboid(-5.0F, 0.0F, -1.0F, 10, 16, 1, var1);
      this.ears = new ModelPart(24, 0);
      this.ears.addCuboid(-3.0F, -6.0F, -1.0F, 6, 6, 1, var1);
      this.head = new ModelPart(0, 0);
      this.head.addCuboid(-4.0F, -8.0F, -4.0F, 8, 8, 8, var1);
      this.head.setPivot(0.0F, 0.0F + var2, 0.0F);
      this.hat = new ModelPart(32, 0);
      this.hat.addCuboid(-4.0F, -8.0F, -4.0F, 8, 8, 8, var1 + 0.5F);
      this.hat.setPivot(0.0F, 0.0F + var2, 0.0F);
      this.body = new ModelPart(16, 16);
      this.body.addCuboid(-4.0F, 0.0F, -2.0F, 8, 12, 4, var1);
      this.body.setPivot(0.0F, 0.0F + var2, 0.0F);
      this.rightArm = new ModelPart(40, 16);
      this.rightArm.addCuboid(-3.0F, -2.0F, -2.0F, 4, 12, 4, var1);
      this.rightArm.setPivot(-5.0F, 2.0F + var2, 0.0F);
      this.leftArm = new ModelPart(40, 16);
      this.leftArm.mirror = true;
      this.leftArm.addCuboid(-1.0F, -2.0F, -2.0F, 4, 12, 4, var1);
      this.leftArm.setPivot(5.0F, 2.0F + var2, 0.0F);
      this.rightLeg = new ModelPart(0, 16);
      this.rightLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, 12, 4, var1);
      this.rightLeg.setPivot(-2.0F, 12.0F + var2, 0.0F);
      this.leftLeg = new ModelPart(0, 16);
      this.leftLeg.mirror = true;
      this.leftLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, 12, 4, var1);
      this.leftLeg.setPivot(2.0F, 12.0F + var2, 0.0F);
   }

   public void render(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.setAngles(var1, var2, var3, var4, var5, var6);
      this.head.render(var6);
      this.body.render(var6);
      this.rightArm.render(var6);
      this.leftArm.render(var6);
      this.rightLeg.render(var6);
      this.leftLeg.render(var6);
      this.hat.render(var6);
   }

   public void setAngles(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.head.yaw = var4 / (180.0F / (float)Math.PI);
      this.head.pitch = var5 / (180.0F / (float)Math.PI);
      this.hat.yaw = this.head.yaw;
      this.hat.pitch = this.head.pitch;
      this.rightArm.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 2.0F * var2 * 0.5F;
      this.leftArm.pitch = MathHelper.cos(var1 * 0.6662F) * 2.0F * var2 * 0.5F;
      this.rightArm.roll = 0.0F;
      this.leftArm.roll = 0.0F;
      this.rightLeg.pitch = MathHelper.cos(var1 * 0.6662F) * 1.4F * var2;
      this.leftLeg.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 1.4F * var2;
      this.rightLeg.yaw = 0.0F;
      this.leftLeg.yaw = 0.0F;
      if (this.riding) {
         this.rightArm.pitch += (float) (-Math.PI / 5);
         this.leftArm.pitch += (float) (-Math.PI / 5);
         this.rightLeg.pitch = (float) (-Math.PI * 2.0 / 5.0);
         this.leftLeg.pitch = (float) (-Math.PI * 2.0 / 5.0);
         this.rightLeg.yaw = (float) (Math.PI / 10);
         this.leftLeg.yaw = (float) (-Math.PI / 10);
      }

      if (this.leftArmPose) {
         this.leftArm.pitch = this.leftArm.pitch * 0.5F - (float) (Math.PI / 10);
      }

      if (this.rightArmPose) {
         this.rightArm.pitch = this.rightArm.pitch * 0.5F - (float) (Math.PI / 10);
      }

      this.rightArm.yaw = 0.0F;
      this.leftArm.yaw = 0.0F;
      if (this.handSwingProgress > -9990.0F) {
         float var7 = this.handSwingProgress;
         this.body.yaw = MathHelper.sin(MathHelper.sqrt(var7) * (float) Math.PI * 2.0F) * 0.2F;
         this.rightArm.pivotZ = MathHelper.sin(this.body.yaw) * 5.0F;
         this.rightArm.pivotX = -MathHelper.cos(this.body.yaw) * 5.0F;
         this.leftArm.pivotZ = -MathHelper.sin(this.body.yaw) * 5.0F;
         this.leftArm.pivotX = MathHelper.cos(this.body.yaw) * 5.0F;
         this.rightArm.yaw = this.rightArm.yaw + this.body.yaw;
         this.leftArm.yaw = this.leftArm.yaw + this.body.yaw;
         this.leftArm.pitch = this.leftArm.pitch + this.body.yaw;
         var7 = 1.0F - this.handSwingProgress;
         var7 *= var7;
         var7 *= var7;
         var7 = 1.0F - var7;
         float var8 = MathHelper.sin(var7 * (float) Math.PI);
         float var9 = MathHelper.sin(this.handSwingProgress * (float) Math.PI) * -(this.head.pitch - 0.7F) * 0.75F;
         this.rightArm.pitch = (float)((double)this.rightArm.pitch - ((double)var8 * 1.2 + (double)var9));
         this.rightArm.yaw = this.rightArm.yaw + this.body.yaw * 2.0F;
         this.rightArm.roll = MathHelper.sin(this.handSwingProgress * (float) Math.PI) * -0.4F;
      }

      if (this.sneaking) {
         this.body.pitch = 0.5F;
         this.rightLeg.pitch -= 0.0F;
         this.leftLeg.pitch -= 0.0F;
         this.rightArm.pitch += 0.4F;
         this.leftArm.pitch += 0.4F;
         this.rightLeg.pivotZ = 4.0F;
         this.leftLeg.pivotZ = 4.0F;
         this.rightLeg.pivotY = 9.0F;
         this.leftLeg.pivotY = 9.0F;
         this.head.pivotY = 1.0F;
      } else {
         this.body.pitch = 0.0F;
         this.rightLeg.pivotZ = 0.0F;
         this.leftLeg.pivotZ = 0.0F;
         this.rightLeg.pivotY = 12.0F;
         this.leftLeg.pivotY = 12.0F;
         this.head.pivotY = 0.0F;
      }

      this.rightArm.roll = this.rightArm.roll + MathHelper.cos(var3 * 0.09F) * 0.05F + 0.05F;
      this.leftArm.roll = this.leftArm.roll - (MathHelper.cos(var3 * 0.09F) * 0.05F + 0.05F);
      this.rightArm.pitch = this.rightArm.pitch + MathHelper.sin(var3 * 0.067F) * 0.05F;
      this.leftArm.pitch = this.leftArm.pitch - MathHelper.sin(var3 * 0.067F) * 0.05F;
   }

   public void renderEars(float var1) {
      this.ears.yaw = this.head.yaw;
      this.ears.pitch = this.head.pitch;
      this.ears.pivotX = 0.0F;
      this.ears.pivotY = 0.0F;
      this.ears.render(var1);
   }

   public void renderCape(float var1) {
      this.cape.render(var1);
   }
}
