package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.model.IMultipassModel;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBoat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityBoat;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 boat renderer. Vanilla boats (modern entities) are drawn by the modern renderer at the legacy matrix; a mod's own boat is
 * drawn the 1.12 way: the boat model turned to the boat's yaw, rocking and tilting when hit.
 */
public class RenderBoat extends Render<EntityBoat> {
    private static final ResourceLocation[] BOAT_TEXTURES = {new ResourceLocation("textures/entity/boat/boat_oak.png"),
            new ResourceLocation("textures/entity/boat/boat_spruce.png"), new ResourceLocation("textures/entity/boat/boat_birch.png"),
            new ResourceLocation("textures/entity/boat/boat_jungle.png"), new ResourceLocation("textures/entity/boat/boat_acacia.png"),
            new ResourceLocation("textures/entity/boat/boat_darkoak.png")};
    protected ModelBase modelBoat = new ModelBoat();

    public RenderBoat(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityBoat entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null && !(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        setupTranslation(x, y, z);
        setupRotation(entity, entityYaw, partialTicks);
        bindEntityTexture(entity);
        modelBoat.render(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    public void setupRotation(EntityBoat boat, float entityYaw, float partialTicks) {
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        float f = boat.getTimeSinceHit() - partialTicks;
        float f1 = boat.getDamageTaken() - partialTicks;
        if (f1 < 0.0F) f1 = 0.0F;
        if (f > 0.0F) GlStateManager.rotate(MathHelper.sin(f) * f * f1 / 10.0F * boat.getForwardDirection(), 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
    }

    public void setupTranslation(double x, double y, double z) {
        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBoat entity) {
        int i = entity.getBoatType().ordinal();
        return BOAT_TEXTURES[i < 0 || i >= BOAT_TEXTURES.length ? 0 : i];
    }

    @Override
    public boolean isMultipass() {
        return true;
    }

    @Override
    public void renderMultipass(EntityBoat entityIn, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entityIn.mcc$entity != null && !(entityIn.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122)) return;
        GlStateManager.pushMatrix();
        setupTranslation(x, y, z);
        setupRotation(entityIn, entityYaw, partialTicks);
        bindEntityTexture(entityIn);
        ((IMultipassModel) modelBoat).renderMultipass(entityIn, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
    }
}
