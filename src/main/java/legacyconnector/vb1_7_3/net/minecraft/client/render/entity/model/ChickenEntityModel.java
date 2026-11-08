package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class ChickenEntityModel extends EntityModel {
   public ModelPart head;
   public ModelPart body;
   public ModelPart rightLeg;
   public ModelPart leftLeg;
   public ModelPart rightWing;
   public ModelPart leftWing;
   public ModelPart beak;
   public ModelPart wattle;

   public ChickenEntityModel() {
      byte var1 = 16;
      this.head = new ModelPart(0, 0);
      this.head.addCuboid(-2.0F, -6.0F, -2.0F, 4, 6, 3, 0.0F);
      this.head.setPivot(0.0F, (float)(-1 + var1), -4.0F);
      this.beak = new ModelPart(14, 0);
      this.beak.addCuboid(-2.0F, -4.0F, -4.0F, 4, 2, 2, 0.0F);
      this.beak.setPivot(0.0F, (float)(-1 + var1), -4.0F);
      this.wattle = new ModelPart(14, 4);
      this.wattle.addCuboid(-1.0F, -2.0F, -3.0F, 2, 2, 2, 0.0F);
      this.wattle.setPivot(0.0F, (float)(-1 + var1), -4.0F);
      this.body = new ModelPart(0, 9);
      this.body.addCuboid(-3.0F, -4.0F, -3.0F, 6, 8, 6, 0.0F);
      this.body.setPivot(0.0F, (float)(0 + var1), 0.0F);
      this.rightLeg = new ModelPart(26, 0);
      this.rightLeg.addCuboid(-1.0F, 0.0F, -3.0F, 3, 5, 3);
      this.rightLeg.setPivot(-2.0F, (float)(3 + var1), 1.0F);
      this.leftLeg = new ModelPart(26, 0);
      this.leftLeg.addCuboid(-1.0F, 0.0F, -3.0F, 3, 5, 3);
      this.leftLeg.setPivot(1.0F, (float)(3 + var1), 1.0F);
      this.rightWing = new ModelPart(24, 13);
      this.rightWing.addCuboid(0.0F, 0.0F, -3.0F, 1, 4, 6);
      this.rightWing.setPivot(-4.0F, (float)(-3 + var1), 0.0F);
      this.leftWing = new ModelPart(24, 13);
      this.leftWing.addCuboid(-1.0F, 0.0F, -3.0F, 1, 4, 6);
      this.leftWing.setPivot(4.0F, (float)(-3 + var1), 0.0F);
   }

   public void render(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.setAngles(var1, var2, var3, var4, var5, var6);
      this.head.render(var6);
      this.beak.render(var6);
      this.wattle.render(var6);
      this.body.render(var6);
      this.rightLeg.render(var6);
      this.leftLeg.render(var6);
      this.rightWing.render(var6);
      this.leftWing.render(var6);
   }

   public void setAngles(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.head.pitch = -(var5 / (180.0F / (float)Math.PI));
      this.head.yaw = var4 / (180.0F / (float)Math.PI);
      this.beak.pitch = this.head.pitch;
      this.beak.yaw = this.head.yaw;
      this.wattle.pitch = this.head.pitch;
      this.wattle.yaw = this.head.yaw;
      this.body.pitch = (float) (Math.PI / 2);
      this.rightLeg.pitch = MathHelper.cos(var1 * 0.6662F) * 1.4F * var2;
      this.leftLeg.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 1.4F * var2;
      this.rightWing.roll = var3;
      this.leftWing.roll = -var3;
   }
}
