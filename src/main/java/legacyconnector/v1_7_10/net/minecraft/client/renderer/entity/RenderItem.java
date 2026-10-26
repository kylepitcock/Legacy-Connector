package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** Item rendering through the modern item renderer, into the active render context. */
public class RenderItem extends Render {
    public boolean renderWithColor = true;
    public float zLevel;
    public static boolean renderInFrame;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityItem item) doRender(item, x, y, z, yaw, partialTicks);
    }

    public void doRender(EntityItem entity, double x, double y, double z, float yaw, float partialTicks) {
        ItemStack stack = entity.getEntityItem();
        var pose = RenderContext.pose();
        if (stack == null || pose == null) return;
        pose.pushPose();
        pose.translate(x, y, z);
        if (!renderInFrame && shouldBob()) pose.translate(0, Math.sin(((entity.mcc$entity == null ? 0 : entity.mcc$entity.tickCount) + partialTicks) / 10.0F) * 0.1F + 0.1F, 0);
        // the GROUND transform already has 1.7.10's dropped sizes (items half a block, blocks a quarter)
        var mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(stack.mcc$modern(), ItemDisplayContext.GROUND, RenderContext.light(), RenderContext.overlay(), pose,
                RenderContext.buffers(), (net.minecraft.world.level.Level) (Object) mc.level, 0);
        pose.popPose();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }

    public void renderItemIntoGUI(FontRenderer font, TextureManager tm, ItemStack stack, int x, int y) {
        var gui = RenderContext.gui();
        if (gui != null && stack != null) gui.renderItem(stack.mcc$modern(), x, y);
    }

    public void renderItemAndEffectIntoGUI(FontRenderer font, TextureManager tm, ItemStack stack, int x, int y) {
        renderItemIntoGUI(font, tm, stack, x, y);
    }

    public void renderItemOverlayIntoGUI(FontRenderer font, TextureManager tm, ItemStack stack, int x, int y) {
        var gui = RenderContext.gui();
        if (gui != null && stack != null) gui.renderItemDecorations(Minecraft.getInstance().font, stack.mcc$modern(), x, y);
    }

    public void renderItemOverlayIntoGUI(FontRenderer font, TextureManager tm, ItemStack stack, int x, int y, String text) {
        var gui = RenderContext.gui();
        if (gui != null && stack != null) gui.renderItemDecorations(Minecraft.getInstance().font, stack.mcc$modern(), x, y, text);
    }

    /** Forge hooks that custom renderers override. */
    public boolean shouldBob() {
        return true;
    }

    public boolean shouldSpreadItems() {
        return true;
    }

    public byte getMiniBlockCount(ItemStack stack, byte original) {
        return original;
    }

    public byte getMiniItemCount(ItemStack stack, byte original) {
        return original;
    }

    private static RenderItem mcc$instance;

    public static synchronized RenderItem getInstance() {
        if (mcc$instance == null) mcc$instance = new RenderItem();
        return mcc$instance;
    }

    public void renderIcon(int x, int y, legacyconnector.v1_7_10.net.minecraft.util.IIcon icon, int width, int height) {
        new legacyconnector.v1_7_10.net.minecraft.client.gui.Gui().drawTexturedModelRectFromIcon(x, y, icon, width, height);
    }

    /**
     * The enchantment glint over a GUI item at (x, y): a 20x20 quad (2 pixels of margin) in the modern glint render type,
     * which scrolls the glint texture and only touches pixels the item drew at this depth (1.7.10 drew it with depth EQUAL).
     */
    public void renderEffect(TextureManager manager, int x, int y) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null) return;
        var vc = buffers.getBuffer(net.minecraft.client.renderer.RenderType.glint());
        var last = pose.last();
        float x0 = x - 2, y0 = y - 2, x1 = x0 + 20, y1 = y0 + 20;
        vc.addVertex(last, x0, y1, zLevel).setUv(0.0F, 1.0F);
        vc.addVertex(last, x1, y1, zLevel).setUv(1.0F, 1.0F);
        vc.addVertex(last, x1, y0, zLevel).setUv(1.0F, 0.0F);
        vc.addVertex(last, x0, y0, zLevel).setUv(0.0F, 0.0F);
    }

    public void renderItemIntoGUI(FontRenderer font, TextureManager tm, ItemStack stack, int x, int y, boolean renderEffect) {
        renderItemIntoGUI(font, tm, stack, x, y);
    }
}
