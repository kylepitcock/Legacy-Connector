package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySlime;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's slime renderer (also the base of mod slimes): the inner model, then the outer model translucent as a render pass,
 * scaled by the slime's size and squished while it jumps.
 */
public class RenderSlime extends RenderLiving {
    private static final ResourceLocation slimeTextures = new ResourceLocation("textures/entity/slime/slime.png");
    private final ModelBase scaleAmount;

    public RenderSlime(ModelBase model, ModelBase outer, float shadowSize) {
        super(model, shadowSize);
        this.scaleAmount = outer;
    }

    protected int shouldRenderPass(EntitySlime slime, int pass, float partialTicks) {
        if (slime.isInvisible()) return 0;
        if (pass == 0) {
            setRenderPassModel(scaleAmount);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            return 1;
        }
        if (pass == 1) {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
        return -1;
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof EntitySlime slime ? shouldRenderPass(slime, pass, partialTicks) : -1;
    }

    /** Scales by the size and squishes (wide and flat while landing, tall while jumping). */
    protected void preRenderCallback(EntitySlime slime, float partialTicks) {
        float size = slime.getSlimeSize();
        float squish = slime.mcc$entity instanceof net.minecraft.world.entity.monster.Slime modern
                ? net.minecraft.util.Mth.lerp(partialTicks, modern.oSquish, modern.squish) : 0.0F;
        float f = squish / (size * 0.5F + 1.0F);
        float inv = 1.0F / (f + 1.0F);
        GL11.glScalef(inv * size, 1.0F / inv * size, inv * size);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntitySlime slime) preRenderCallback(slime, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntitySlime slime) {
        return slimeTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntitySlime slime ? getEntityTexture(slime) : slimeTextures;
    }
}
