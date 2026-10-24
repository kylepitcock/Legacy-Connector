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
        pose.scale(0.5F, 0.5F, 0.5F);
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

    public void renderItemIntoGUI(FontRenderer font, TextureManager tm, ItemStack stack, int x, int y, boolean renderEffect) {
        renderItemIntoGUI(font, tm, stack, x, y);
    }
}
