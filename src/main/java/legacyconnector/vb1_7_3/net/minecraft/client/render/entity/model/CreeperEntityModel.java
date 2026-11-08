package legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model;

import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.util.math.MathHelper;

/** Beta vanilla creeper model geometry and animation, used by original subclass callbacks. */
public class CreeperEntityModel extends EntityModel {
   public ModelPart head;
   public ModelPart hair;
   public ModelPart body;
   public ModelPart leg1;
   public ModelPart leg2;
   public ModelPart leg3;
   public ModelPart leg4;

   public CreeperEntityModel() {
      this(0.0F);
   }

   public CreeperEntityModel(float var1) {
      int var2 = 4;
      this.head = new ModelPart(0, 0);
      this.head.addCuboid(-4.0F, -8.0F, -4.0F, 8, 8, 8, var1);
      this.head.setPivot(0.0F, (float)var2, 0.0F);
      this.hair = new ModelPart(32, 0);
      this.hair.addCuboid(-4.0F, -8.0F, -4.0F, 8, 8, 8, var1 + 0.5F);
      this.hair.setPivot(0.0F, (float)var2, 0.0F);
      this.body = new ModelPart(16, 16);
      this.body.addCuboid(-4.0F, 0.0F, -2.0F, 8, 12, 4, var1);
      this.body.setPivot(0.0F, (float)var2, 0.0F);
      this.leg1 = new ModelPart(0, 16);
      this.leg1.addCuboid(-2.0F, 0.0F, -2.0F, 4, 6, 4, var1);
      this.leg1.setPivot(-2.0F, (float)(12 + var2), 4.0F);
      this.leg2 = new ModelPart(0, 16);
      this.leg2.addCuboid(-2.0F, 0.0F, -2.0F, 4, 6, 4, var1);
      this.leg2.setPivot(2.0F, (float)(12 + var2), 4.0F);
      this.leg3 = new ModelPart(0, 16);
      this.leg3.addCuboid(-2.0F, 0.0F, -2.0F, 4, 6, 4, var1);
      this.leg3.setPivot(-2.0F, (float)(12 + var2), -4.0F);
      this.leg4 = new ModelPart(0, 16);
      this.leg4.addCuboid(-2.0F, 0.0F, -2.0F, 4, 6, 4, var1);
      this.leg4.setPivot(2.0F, (float)(12 + var2), -4.0F);
   }

   public void render(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.setAngles(var1, var2, var3, var4, var5, var6);
      this.head.render(var6);
      this.body.render(var6);
      this.leg1.render(var6);
      this.leg2.render(var6);
      this.leg3.render(var6);
      this.leg4.render(var6);
   }

   public void setAngles(float var1, float var2, float var3, float var4, float var5, float var6) {
      this.head.yaw = var4 / (180.0F / (float)Math.PI);
      this.head.pitch = var5 / (180.0F / (float)Math.PI);
      this.leg1.pitch = MathHelper.cos(var1 * 0.6662F) * 1.4F * var2;
      this.leg2.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 1.4F * var2;
      this.leg3.pitch = MathHelper.cos(var1 * 0.6662F + (float) Math.PI) * 1.4F * var2;
      this.leg4.pitch = MathHelper.cos(var1 * 0.6662F) * 1.4F * var2;
   }
}
