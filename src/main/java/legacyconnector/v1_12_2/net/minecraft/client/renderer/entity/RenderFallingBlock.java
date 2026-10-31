package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityFallingBlock;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/** 1.12.2 falling block renderer: the modern falling block renderer (the block of its state) at the legacy matrix. */
public class RenderFallingBlock extends Render<EntityFallingBlock> {
    public RenderFallingBlock(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityFallingBlock entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null) LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFallingBlock entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
