package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.ModelLlama;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityLlama;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/** 1.12.2 llama renderer: the four coat variants on the 1.12 llama model; vanilla llamas (modern entities) are drawn by the modern renderer. */
public class RenderLlama extends RenderLiving<EntityLlama> {
    private static final ResourceLocation[] LLAMA_TEXTURES = {
            new ResourceLocation("textures/entity/llama/creamy.png"),
            new ResourceLocation("textures/entity/llama/white.png"),
            new ResourceLocation("textures/entity/llama/brown.png"),
            new ResourceLocation("textures/entity/llama/gray.png")};

    public RenderLlama(RenderManager renderManagerIn) {
        super(renderManagerIn, new ModelLlama(0.0F), 0.7F);
    }

    @Override
    public void doRender(EntityLlama entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null && !(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)
                && getClass() == RenderLlama.class) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLlama entity) {
        return LLAMA_TEXTURES[Math.floorMod(entity.getVariant(), LLAMA_TEXTURES.length)];
    }
}
