package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBat;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityBat;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's bat renderer: the bat model at 0.35 scale, bobbing while flying. Note that the modern bat texture has the
 * 1.20.3 layout, so the legacy model only matches mod textures.
 */
public class RenderBat extends RenderLiving {
    private static final ResourceLocation batTextures = new ResourceLocation("textures/entity/bat.png");
    private int renderedBatSize;

    public RenderBat() {
        super(new ModelBat(), 0.25F);
        this.renderedBatSize = ((ModelBat) this.mainModel).getBatSize();
    }

    public void doRender(EntityBat bat, double x, double y, double z, float yaw, float partialTicks) {
        int size = mainModel instanceof ModelBat m ? m.getBatSize() : renderedBatSize;
        if (size != renderedBatSize) {
            renderedBatSize = size;
            mainModel = new ModelBat();
        }
        super.doRender((EntityLiving) bat, x, y, z, yaw, partialTicks);
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityBat bat) doRender(bat, x, y, z, yaw, partialTicks);
        else super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityBat bat) {
        return batTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityBat bat ? getEntityTexture(bat) : batTextures;
    }

    protected void preRenderCallback(EntityBat bat, float partialTicks) {
        GL11.glScalef(0.35F, 0.35F, 0.35F);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        if (entity instanceof EntityBat bat) preRenderCallback(bat, partialTicks);
    }

    protected void renderLivingAt(EntityBat bat, double x, double y, double z) {
        super.renderLivingAt(bat, x, y, z);
    }

    protected void rotateCorpse(EntityBat bat, float age, float bodyYaw, float partialTicks) {
        if (!bat.getIsBatHanging()) GL11.glTranslatef(0.0F, (float) Math.cos(age * 0.3F) * 0.1F, 0.0F);
        else GL11.glTranslatef(0.0F, -0.1F, 0.0F);
        super.rotateCorpse(bat, age, bodyYaw, partialTicks);
    }

    @Override
    protected void rotateCorpse(EntityLivingBase entity, float age, float bodyYaw, float partialTicks) {
        if (entity instanceof EntityBat bat) rotateCorpse(bat, age, bodyYaw, partialTicks);
        else super.rotateCorpse(entity, age, bodyYaw, partialTicks);
    }
}
