package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityTNTPrimed;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/** 1.12.2 primed TNT renderer: the modern TNT renderer (swelling and flashing block) at the legacy matrix. */
public class RenderTNTPrimed extends Render<EntityTNTPrimed> {
    public RenderTNTPrimed(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityTNTPrimed entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null) LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTNTPrimed entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
