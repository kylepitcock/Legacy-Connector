package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 dropped-item renderer. The item is drawn by the modern item entity renderer (bobbing, spinning and the copies of a stack are
 * the game's own), at the legacy matrix; mods extend it for pedestals and floating items.
 */
public class RenderEntityItem extends Render<EntityItem> {
    private final RenderItem itemRenderer;
    private final Random random = new Random();

    public RenderEntityItem(RenderManager renderManagerIn, RenderItem itemRendererIn) {
        super(renderManagerIn);
        this.itemRenderer = itemRendererIn;
        this.shadowSize = 0.15F;
        this.shadowOpaque = 0.75F;
    }

    /** How many copies 1.12 drew of a stack of this size. */
    protected int getModelCount(ItemStack stack) {
        int count = stack.getCount();
        if (count > 48) return 5;
        if (count > 32) return 4;
        if (count > 16) return 3;
        return count > 1 ? 2 : 1;
    }

    @Override
    public void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.mcc$entity != null) LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityItem entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    public boolean shouldSpreadItems() {
        return true;
    }

    public boolean shouldBob() {
        return true;
    }
}
