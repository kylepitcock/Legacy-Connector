package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla model geometry and animation, used by original subclass callbacks. */
public class SheepEntityModel extends QuadrupedEntityModel {
   public SheepEntityModel() {
      super(12, 0.0F);
      this.head = new ModelPart(0, 0);
      this.head.addCuboid(-3.0F, -4.0F, -6.0F, 6, 6, 8, 0.0F);
      this.head.setPivot(0.0F, 6.0F, -8.0F);
      this.body = new ModelPart(28, 8);
      this.body.addCuboid(-4.0F, -10.0F, -7.0F, 8, 16, 6, 0.0F);
      this.body.setPivot(0.0F, 5.0F, 2.0F);
   }
}
