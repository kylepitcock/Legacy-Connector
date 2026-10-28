package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.ModelManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.ItemColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.client.Models1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's item renderer: items are drawn by the game's item renderer into the render context (a screen's GuiGraphics for the GUI
 * methods, the current matrix and buffers elsewhere), so models, overrides, tints and the enchantment glint are the game's.
 */
public class RenderItem {
    private static RenderItem instance;
    public float zLevel;
    private final ItemModelMesher itemModelMesher;
    private final TextureManager textureManager;
    private final ItemColors itemColors;
    private boolean notRenderingEffectsInGUI = true;

    public RenderItem(TextureManager p_i46552_1_, ModelManager p_i46552_2_, ItemColors p_i46552_3_) {
        textureManager = p_i46552_1_;
        itemModelMesher = new ItemModelMesher(p_i46552_2_);
        itemColors = p_i46552_3_;
    }

    /** The game's item renderer (Minecraft.getRenderItem()). */
    public static synchronized RenderItem mcc$instance() {
        if (instance == null) instance = new RenderItem(TextureManager.mcc$instance(), ModelManager.mcc$instance(), ItemColors.mcc$instance());
        return instance;
    }

    public ItemModelMesher getItemModelMesher() {
        return itemModelMesher;
    }

    public void isNotRenderingEffectsInGUI(boolean isNot) {
        notRenderingEffectsInGUI = isNot;
    }

    private static net.minecraft.client.renderer.entity.ItemRenderer modernRenderer() {
        return net.minecraft.client.Minecraft.getInstance().getItemRenderer();
    }

    /** Draws a model at the current matrix with no display transform (the caller applied it). */
    public void renderItem(ItemStack stack, IBakedModel model) {
        if (stack == null || stack.isEmpty() || model == null) return;
        var pose = RenderContext.pose();
        if (pose == null) return;
        pose.pushPose();
        try {
            // (the modern renderer moves the model by half a block itself, as vanilla's renderItem did)
            modernRenderer().render(Stacks1122.toModern(stack), ItemDisplayContext.NONE, false, pose, RenderContext.buffers(), RenderContext.vertexLight(),
                    RenderContext.overlay(), Models1122.modern(model));
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing an item model", e);
        } finally {
            pose.popPose();
        }
    }

    public boolean shouldRenderItemIn3D(ItemStack stack) {
        IBakedModel model = itemModelMesher.getItemModel(stack);
        return model != null && model.isGui3d();
    }

    public void renderItem(ItemStack stack, ItemCameraTransforms.TransformType cameraTransformType) {
        renderStatic(stack, cameraTransformType, false, null);
    }

    public IBakedModel getItemModelWithOverrides(ItemStack stack, World worldIn, EntityLivingBase entitylivingbaseIn) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        var living = entitylivingbaseIn != null && entitylivingbaseIn.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
        return Models1122.legacy(modernRenderer().getModel(Stacks1122.toModern(stack), level, living, 0));
    }

    public void renderItem(ItemStack stack, EntityLivingBase entitylivingbaseIn, ItemCameraTransforms.TransformType transform, boolean leftHanded) {
        renderStatic(stack, transform, leftHanded, entitylivingbaseIn);
    }

    private void renderStatic(ItemStack stack, ItemCameraTransforms.TransformType type, boolean leftHanded, EntityLivingBase entity) {
        if (stack == null || stack.isEmpty()) return;
        var pose = RenderContext.pose();
        if (pose == null) return;
        try {
            var living = entity != null && entity.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
            var modernStack = Stacks1122.toModern(stack);
            var model = modernRenderer().getModel(modernStack, net.minecraft.client.Minecraft.getInstance().level, living, 0);
            modernRenderer().render(modernStack, type.mcc$modern(), leftHanded, pose, RenderContext.buffers(), RenderContext.vertexLight(), RenderContext.overlay(), model);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing an item", e);
        }
    }

    public void renderItemIntoGUI(ItemStack stack, int x, int y) {
        renderGui(stack, x, y);
    }

    public void renderItemAndEffectIntoGUI(ItemStack stack, int xPosition, int yPosition) {
        renderGui(stack, xPosition, yPosition);
    }

    public void renderItemAndEffectIntoGUI(EntityLivingBase entity, ItemStack stack, int x, int y) {
        renderGui(stack, x, y);
    }

    /** Through the screen's GuiGraphics (the item is drawn at once, in front of what came before, at the legacy z level). */
    private void renderGui(ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        var gui = RenderContext.gui();
        if (gui == null) return;
        gui.pose().pushPose();
        try {
            if (zLevel != 0) gui.pose().translate(0.0F, 0.0F, zLevel);
            gui.renderItem(Stacks1122.toModern(stack), x, y);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing an item in a screen", e);
        } finally {
            gui.pose().popPose();
        }
    }

    /** A solid rectangle of one colour (durability bars). */
    public void draw(BufferBuilder renderer, int x, int y, int width, int height, int red, int green, int blue, int alpha) {
        renderer.begin(7, legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(x, y, 0.0D).color(red, green, blue, alpha).endVertex();
        renderer.pos(x, y + height, 0.0D).color(red, green, blue, alpha).endVertex();
        renderer.pos(x + width, y + height, 0.0D).color(red, green, blue, alpha).endVertex();
        renderer.pos(x + width, y, 0.0D).color(red, green, blue, alpha).endVertex();
        Tessellator.getInstance().draw();
    }

    // ------------------------------------------------------------------ overlays and raw model drawing (area R1)

    /** The stack count, durability bar and cooldown shade over a slot (the modern GUI's item decorations). */
    public void renderItemOverlays(legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer fr, ItemStack stack, int xPosition, int yPosition) {
        renderItemOverlayIntoGUI(fr, stack, xPosition, yPosition, null);
    }

    public void renderItemOverlayIntoGUI(legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer fr, ItemStack stack, int xPosition, int yPosition, String text) {
        if (stack == null || stack.isEmpty()) return;
        var gui = RenderContext.gui();
        if (gui == null) return;
        gui.pose().pushPose();
        try {
            if (zLevel != 0) gui.pose().translate(0.0F, 0.0F, zLevel);
            gui.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, Stacks1122.toModern(stack), xPosition, yPosition, text);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing item overlays", e);
        } finally {
            gui.pose().popPose();
        }
    }

    /** A model's quads at the current matrix with the stack's tints (no display transform, no glint). */
    public void renderModel(IBakedModel model, ItemStack stack) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null || model == null) return;
        try {
            var buffer = buffers.getBuffer(net.minecraft.client.renderer.Sheets.translucentItemSheet());
            modernRenderer().renderModelLists(Models1122.modern(model), stack == null ? net.minecraft.world.item.ItemStack.EMPTY : Stacks1122.toModern(stack),
                    RenderContext.vertexLight(), RenderContext.overlay(), pose, buffer);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing an item model's quads", e);
        }
    }

    /** A model's quads in one ARGB colour. */
    public void renderModel(IBakedModel model, int color) {
        mcc$quads(model, net.minecraft.client.renderer.Sheets.translucentItemSheet(), color);
    }

    /** The enchantment glint over a model. */
    public void renderEffect(IBakedModel model) {
        mcc$quads(model, net.minecraft.client.renderer.RenderType.glint(), -1);
    }

    private void mcc$quads(IBakedModel model, net.minecraft.client.renderer.RenderType type, int color) {
        var pose = RenderContext.pose();
        var buffers = RenderContext.buffers();
        if (pose == null || buffers == null || model == null) return;
        try {
            var modern = Models1122.modern(model);
            var buffer = buffers.getBuffer(type);
            var random = net.minecraft.util.RandomSource.create();
            float a = (color >> 24 & 255) / 255.0F, r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
            if (color == -1) a = r = g = b = 1.0F;
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                random.setSeed(42L);
                for (var quad : modern.getQuads(null, d, random)) buffer.putBulkData(pose.last(), quad, r, g, b, a, RenderContext.vertexLight(), RenderContext.overlay());
            }
            random.setSeed(42L);
            for (var quad : modern.getQuads(null, null, random)) buffer.putBulkData(pose.last(), quad, r, g, b, a, RenderContext.vertexLight(), RenderContext.overlay());
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing an item model's quads", e);
        }
    }
}
