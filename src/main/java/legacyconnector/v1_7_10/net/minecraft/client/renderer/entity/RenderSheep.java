package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySheep;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's sheep renderer (also the base of mod sheep): the body, then the fleece as a render pass in the fleece colour
 * (cycling through all colours for a sheep named jeb_), left out once sheared.
 */
public class RenderSheep extends RenderLiving {
    private static final ResourceLocation shearedSheepTextures = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
    private static final ResourceLocation sheepTextures = new ResourceLocation("textures/entity/sheep/sheep.png");

    public RenderSheep(ModelBase model, ModelBase fleece, float shadowSize) {
        super(model, shadowSize);
        setRenderPassModel(fleece);
    }

    protected int shouldRenderPass(EntitySheep sheep, int pass, float partialTicks) {
        if (pass == 0 && !sheep.getSheared()) {
            bindTexture(shearedSheepTextures);
            float[] rgb;
            if (sheep.hasCustomNameTag() && "jeb_".equals(sheep.getCustomNameTag())) {
                int ticks = (sheep.mcc$entity == null ? 0 : sheep.mcc$entity.tickCount) / 25 + sheep.getEntityId();
                int n = EntitySheep.fleeceColorTable.length;
                float f = ((sheep.mcc$entity == null ? 0 : sheep.mcc$entity.tickCount) % 25 + partialTicks) / 25.0F;
                float[] a = EntitySheep.fleeceColorTable[ticks % n], b = EntitySheep.fleeceColorTable[(ticks + 1) % n];
                rgb = new float[] {a[0] * (1.0F - f) + b[0] * f, a[1] * (1.0F - f) + b[1] * f, a[2] * (1.0F - f) + b[2] * f};
            } else {
                int color = sheep.getFleeceColor();
                rgb = EntitySheep.fleeceColorTable[color >= 0 && color < EntitySheep.fleeceColorTable.length ? color : 0];
            }
            GL11.glColor3f(rgb[0], rgb[1], rgb[2]);
            return 1;
        }
        if (pass == 1) GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return -1;
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof EntitySheep sheep ? shouldRenderPass(sheep, pass, partialTicks) : -1;
    }

    protected ResourceLocation getEntityTexture(EntitySheep sheep) {
        return sheepTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntitySheep sheep ? getEntityTexture(sheep) : sheepTextures;
    }
}
