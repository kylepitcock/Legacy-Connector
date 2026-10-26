package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBase;
import legacyconnector.v1_7_10.net.minecraft.client.model.ModelMinecart;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityMinecart;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/**
 * 1.7.10's minecart renderer: the cart model, tilted by its pitch and shaken by damage, with the block it carries drawn
 * inside at 3/4 size ({@link #func_147910_a}, which mods override to draw their own content).
 */
public class RenderMinecart extends Render {
    private static final ResourceLocation minecartTextures = new ResourceLocation("textures/entity/minecart.png");
    protected ModelBase modelMinecart = new ModelMinecart();
    protected final RenderBlocks field_94145_f;

    public RenderMinecart() {
        this.shadowSize = 0.5F;
        this.field_94145_f = new RenderBlocks();
    }

    public void doRender(EntityMinecart cart, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        bindEntityTexture(cart);
        long seed = cart.getEntityId() * 493286711L;
        seed = seed * seed * 4392167121L + seed * 98761L;
        float jx = (((seed >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float jy = (((seed >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float jz = (((seed >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        GL11.glTranslatef(jx, jy, jz);
        float pitch = Entity.mcc$get$prevRotationPitch(cart) + (Entity.mcc$get$rotationPitch(cart) - Entity.mcc$get$prevRotationPitch(cart)) * partialTicks;
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-pitch, 0.0F, 0.0F, 1.0F);
        float rolling = cart.getRollingAmplitude() - partialTicks;
        float damage = Math.max(0.0F, cart.getDamage() - partialTicks);
        if (rolling > 0.0F) GL11.glRotatef((float) Math.sin(rolling) * rolling * damage / 10.0F * cart.getRollingDirection(), 1.0F, 0.0F, 0.0F);
        int offset = cart.getDisplayTileOffset();
        Block block = cart.getDisplayTile();
        int meta = cart.getDisplayTileData();
        if (block != null && block.getRenderType() != -1) {
            GL11.glPushMatrix();
            bindTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);
            GL11.glScalef(0.75F, 0.75F, 0.75F);
            GL11.glTranslatef(0.0F, offset / 16.0F, 0.0F);
            func_147910_a(cart, partialTicks, block, meta);
            GL11.glPopMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            bindEntityTexture(cart);
        }
        GL11.glScalef(-1.0F, -1.0F, 1.0F);
        modelMinecart.render(cart, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityMinecart cart) doRender(cart, x, y, z, yaw, partialTicks);
    }

    protected ResourceLocation getEntityTexture(EntityMinecart cart) {
        return minecartTextures;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof EntityMinecart cart ? getEntityTexture(cart) : minecartTextures;
    }

    /** Draws the block the cart carries (centred, a unit cube). */
    protected void func_147910_a(EntityMinecart cart, float partialTicks, Block block, int meta) {
        float brightness = cart.getBrightness(partialTicks);
        GL11.glPushMatrix();
        field_94145_f.renderBlockAsItem(block, meta, brightness);
        GL11.glPopMatrix();
    }
}
