package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class SkeletonEntityModel extends ZombieEntityModel {
   public SkeletonEntityModel() {
      float var1 = 0.0F;
      this.rightArm = new ModelPart(40, 16);
      this.rightArm.addCuboid(-1.0F, -2.0F, -1.0F, 2, 12, 2, var1);
      this.rightArm.setPivot(-5.0F, 2.0F, 0.0F);
      this.leftArm = new ModelPart(40, 16);
      this.leftArm.mirror = true;
      this.leftArm.addCuboid(-1.0F, -2.0F, -1.0F, 2, 12, 2, var1);
      this.leftArm.setPivot(5.0F, 2.0F, 0.0F);
      this.rightLeg = new ModelPart(0, 16);
      this.rightLeg.addCuboid(-1.0F, 0.0F, -1.0F, 2, 12, 2, var1);
      this.rightLeg.setPivot(-2.0F, 12.0F, 0.0F);
      this.leftLeg = new ModelPart(0, 16);
      this.leftLeg.mirror = true;
      this.leftLeg.addCuboid(-1.0F, 0.0F, -1.0F, 2, 12, 2, var1);
      this.leftLeg.setPivot(2.0F, 12.0F, 0.0F);
   }
}
