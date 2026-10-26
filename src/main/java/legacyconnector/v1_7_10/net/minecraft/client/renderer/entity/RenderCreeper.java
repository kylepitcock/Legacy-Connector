package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelCreeper;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityCreeper;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * Creeper renderer (also the base of mod creepers): swells and flashes before exploding; a charged creeper gets the glowing
 * armour layer as a render pass (its scrolling texture matrix has no core-profile equivalent, so it is drawn unscrolled).
 */
public class RenderCreeper extends RenderLiving {
    private static final ResourceLocation armoredCreeperTextures = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
    private static final ResourceLocation CREEPER = new ResourceLocation("textures/entity/creeper/creeper.png");
    private final ModelBase creeperModel = new ModelCreeper(2.0F);

    public RenderCreeper() {
        super(new ModelCreeper(), 0.5F);
    }

    protected void preRenderCallback(EntityCreeper creeper, float partialTicks) {
        float flash = creeper.getCreeperFlashIntensity(partialTicks);
        float wobble = 1.0F + (float) Math.sin(flash * 100.0F) * flash * 0.01F;
        flash = Math.max(0.0F, Math.min(1.0F, flash));
        flash *= flash;
        flash *= flash;
        float xz = (1.0F + flash * 0.4F) * wobble;
        float y = (1.0F + flash * 0.1F) / wobble;
        GL11.glScalef(xz, y, xz);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntityCreeper creeper) preRenderCallback(creeper, partialTicks);
    }

    /** The white flash while fusing, as an ARGB overlay colour (0 when not flashing). */
    protected int getColorMultiplier(EntityCreeper creeper, float brightness, float partialTicks) {
        float flash = creeper.getCreeperFlashIntensity(partialTicks);
        if ((int) (flash * 10.0F) % 2 == 0) return 0;
        int alpha = Math.max(0, Math.min(255, (int) (flash * 0.2F * 255.0F)));
        return alpha << 24 | 255 << 16 | 255 << 8 | 255;
    }

    @Override
    protected int getColorMultiplier(EntityLivingBase entity, float brightness, float partialTicks) {
        return entity instanceof EntityCreeper creeper ? getColorMultiplier(creeper, brightness, partialTicks) : 0;
    }

    protected int shouldRenderPass(EntityCreeper creeper, int pass, float partialTicks) {
        if (creeper.getPowered()) {
            if (pass == 1) {
                GL11.glDepthMask(!creeper.isInvisible());
                bindTexture(armoredCreeperTextures);
                setRenderPassModel(creeperModel);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(0.5F, 0.5F, 0.5F, 1.0F);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                return 1;
            }
            if (pass == 2) {
                GL11.glDepthMask(true);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        return -1;
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof EntityCreeper creeper ? shouldRenderPass(creeper, pass, partialTicks) : -1;
    }

    protected int inheritRenderPass(EntityCreeper creeper, int pass, float partialTicks) {
        return -1;
    }

    @Override
    protected int inheritRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        return -1;
    }

    protected ResourceLocation getEntityTexture(EntityCreeper creeper) {
        return CREEPER;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityCreeper creeper ? getEntityTexture(creeper) : CREEPER;
    }
}
