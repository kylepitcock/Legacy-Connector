package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelMinecart;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureMap;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper;
import org.kylepitcock.legacyconnector.v1_12_2.client.LegacyEntityRenderer1122;

/**
 * 1.12.2 minecart renderer. Vanilla carts (modern entities) are drawn by the modern minecart renderer at the legacy matrix; mod carts are
 * drawn the 1.12 way: the cart model turned to the cart's yaw and pitch, shaking when hit, with its display block
 * ({@link #renderCartContents}) inside.
 */
public class RenderMinecart<T extends EntityMinecart> extends Render<T> {
    private static final ResourceLocation MINECART_TEXTURES = new ResourceLocation("textures/entity/minecart.png");
    protected ModelBase modelMinecart = new ModelMinecart();

    public RenderMinecart(RenderManager renderManagerIn) {
        super(renderManagerIn);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity.mcc$entity instanceof org.kylepitcock.legacyconnector.v1_12_2.entity.Host1122) && entity.mcc$entity != null) {
            LegacyEntityRenderer1122.drawModern(entity.mcc$entity, x, y, z, entityYaw, partialTicks);
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        bindEntityTexture(entity);
        float yaw = Entity.mcc$get$prevRotationYaw(entity) + (Entity.mcc$get$rotationYaw(entity) - Entity.mcc$get$prevRotationYaw(entity)) * partialTicks;
        float pitch = Entity.mcc$get$prevRotationPitch(entity) + (Entity.mcc$get$rotationPitch(entity) - Entity.mcc$get$prevRotationPitch(entity)) * partialTicks;
        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 0.0F, 0.0F, 1.0F);
        float roll = (float) entity.getRollingAmplitude() - partialTicks;
        float damage = Math.max(0.0F, entity.getDamage() - partialTicks);
        if (roll > 0.0F) GlStateManager.rotate(MathHelper.sin(roll) * roll * damage / 10.0F, 1.0F, 0.0F, 0.0F);
        IBlockState contents = mcc$displayTile(entity);
        if (contents != null) {
            GlStateManager.pushMatrix();
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(-0.5F, (float) (mcc$displayOffset(entity) - 8) / 16.0F, 0.5F);
            renderCartContents(entity, partialTicks, contents);
            GlStateManager.popMatrix();
            bindEntityTexture(entity);
        }
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        modelMinecart.render(entity, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /** The cart's display block (EntityMinecart.getDisplayTile of the mod's class), or null. */
    private static IBlockState mcc$displayTile(EntityMinecart cart) {
        try {
            Object o = cart.getClass().getMethod("getDisplayTile").invoke(cart);
            if (!(o instanceof IBlockState state)) return null;
            var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
            return modern == null || modern.isAir() ? null : state;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static int mcc$displayOffset(EntityMinecart cart) {
        try {
            return (int) cart.getClass().getMethod("getDisplayTileOffset").invoke(cart);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 6;
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return MINECART_TEXTURES;
    }

    protected void renderCartContents(T p_188319_1_, float partialTicks, IBlockState p_188319_3_) {
        GlStateManager.pushMatrix();
        legacyconnector.v1_12_2.net.minecraft.client.renderer.BlockRendererDispatcher.mcc$instance().renderBlockBrightness(p_188319_3_, p_188319_1_.getBrightness());
        GlStateManager.popMatrix();
    }
}
