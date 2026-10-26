package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer;
import legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType;
import legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import legacyconnector.v1_7_10.net.minecraftforge.client.MinecraftForgeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * Items with a Forge {@link IItemRenderer}: the legacy renderer draws them, in 1.7.10's conventions. The item model is
 * {@code builtin/entity} with item/generated display transforms, so this renderer works in a 0..1 square (y up):
 * <ul>
 *     <li>INVENTORY: 3D renderers ({@code INVENTORY_BLOCK}) get a block centred at the origin, turned like a block in a
 *     slot; 2D renderers draw in 0..16 pixels with y down.</li>
 *     <li>ENTITY: centred at the origin. EQUIPPED / EQUIPPED_FIRST_PERSON: the 0..1 cube.</li>
 * </ul>
 * A renderer that does not handle the type leaves the item invisible in that context.
 */
public class LegacyItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final RenderBlocks renderBlocks = new RenderBlocks();
    private EntityItem entityItem;
    private Object entityItemLevel;

    public LegacyItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        if (view == null || view.getItem() == null) return;
        ItemRenderType type = switch (context) {
            case GUI -> ItemRenderType.INVENTORY;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> ItemRenderType.EQUIPPED_FIRST_PERSON;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> ItemRenderType.EQUIPPED;
            default -> ItemRenderType.ENTITY;
        };
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(view, type);
        // 1.7.10 asked the item's IItemRenderer first and the block's rendering handler only without one (Thaumcraft's banner
        // handler draws the plain crimson banner, its item renderer the stack's colour and aspect)
        if (renderer == null && view.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemBlock ib
                && renderIsbrhItem(ib, view, context, pose, buffers, light, overlay)) return;
        if (renderer == null && type != ItemRenderType.ENTITY) {
            type = ItemRenderType.ENTITY; // e.g. renderers that only draw dropped/held forms
            renderer = MinecraftForgeClient.getItemRenderer(view, type);
        }
        if (renderer == null) {
            renderIcon(view, pose, buffers, light, overlay);
            return;
        }
        if (context == ItemDisplayContext.GUI && LOGGED.add("gui-pose:" + renderer.getClass().getName())) {
            // Diagnostic: the matrix a legacy renderer starts from in a slot (translation, scale) - items drawn away from their slot
            var m = pose.last().pose();
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").info("GUI item {} ({}): pose translate ({}, {}, {}) scale ({}, {}, {}), type {}",
                    renderer.getClass().getName(), view.getItem().getClass().getSimpleName(), m.m30(), m.m31(), m.m32(), m.m00(), m.m11(), m.m22(), type);
        }
        pose.pushPose();
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            Object[] data;
            switch (type) {
                case INVENTORY -> {
                    if (helper(renderer, type, view, ItemRendererHelper.INVENTORY_BLOCK)) {
                        pose.translate(0.5, 0.5, 0.5);
                        pose.scale(0.625F, 0.625F, 0.625F);
                        pose.mulPose(Axis.XP.rotationDegrees(30));
                        pose.mulPose(Axis.YP.rotationDegrees(-45)); // 1.7.10: rotate(45) then rotate(-90) about Y
                    } else {
                        pose.translate(0, 1, 0);
                        pose.scale(1 / 16F, -1 / 16F, 1 / 16F);
                    }
                    data = new Object[] {renderBlocks};
                }
                case ENTITY -> {
                    pose.translate(0.5, 0.5, 0.5);
                    if (helper(renderer, type, view, ItemRendererHelper.BLOCK_3D)) pose.scale(0.5F, 0.5F, 0.5F);
                    data = new Object[] {renderBlocks, entityItem(view)};
                }
                default -> {
                    if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                        FirstPersonPose.apply(pose, helper(renderer, type, view, ItemRendererHelper.EQUIPPED_BLOCK));
                        legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().entityRenderer.itemRenderer.mcc$setEquipped(FirstPersonPose.equipProgress());
                    }
                    data = new Object[] {renderBlocks, Minecraft.getInstance().player == null ? null : EntityBridge.wrap(Minecraft.getInstance().player)};
                }
            }
            renderer.renderItem(type, view, data);
        } catch (RuntimeException | LinkageError e) {
            // a broken legacy renderer must not take the inventory down
            logOnce(renderer.getClass().getName(), e);
        } finally {
            RenderContext.end();
            pose.popPose();
        }
    }

    /**
     * 1.7.10 drew an item as its icon wherever its IItemRenderer declined the type (Fisk's Heroes' suit pieces have a renderer
     * for locked suits in slots only, and were invisible when dropped, worn on a stand or held): the icon as a sprite one pixel
     * thick, where item/generated puts it.
     */
    private static void renderIcon(ItemStack view, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var item = view.getItem();
        pose.pushPose();
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            var textures = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getTextureManager();
            textures.bindTexture(textures.getResourceLocation(view.getItemSpriteNumber()));
            pose.translate(0, 0, 8.5F / 16F);
            int passes = item.requiresMultipleRenderPasses() ? item.getRenderPasses(view.getItemDamage()) : 1;
            for (int pass = 0; pass < passes; pass++) {
                var icon = item.requiresMultipleRenderPasses() ? item.getIcon(view, pass) : view.getIconIndex();
                if (icon == null) continue;
                int color = item.getColorFromItemStack(view, pass);
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor4f((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
                legacyconnector.v1_7_10.net.minecraft.client.renderer.ItemRenderer.renderItemIn2D(legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.instance,
                        icon.getMinU(), icon.getMinV(), icon.getMaxU(), icon.getMaxV(), icon.getIconWidth(), icon.getIconHeight(), 1F / 16F);
            }
        } catch (RuntimeException | LinkageError e) {
            logOnce(item.getClass().getName() + "#icon", e);
        } finally {
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor4f(1F, 1F, 1F, 1F);
            RenderContext.end();
            pose.popPose();
        }
    }

    /** A block drawn by an ISimpleBlockRenderingHandler, in inventories/hands/drops (3D handlers only). */
    private boolean renderIsbrhItem(legacyconnector.v1_7_10.net.minecraft.item.ItemBlock ib, ItemStack view, ItemDisplayContext context,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var block = ib.mcc$block();
        int renderId;
        try {
            renderId = block.getRenderType();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
        var handler = legacyconnector.v1_7_10.cpw.mods.fml.client.registry.RenderingRegistry.mcc$blockHandler(renderId);
        if (handler == null) return false;
        pose.pushPose();
        RenderContext.beginWorld(pose, buffers, light, overlay);
        try {
            if (!handler.shouldRender3DInInventory(renderId)) return false;
            RenderContext.bindTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            pose.translate(0.5, 0.5, 0.5);
            if (context == ItemDisplayContext.GUI) {
                pose.scale(0.625F, 0.625F, 0.625F);
                pose.mulPose(Axis.XP.rotationDegrees(30));
                pose.mulPose(Axis.YP.rotationDegrees(-45)); // 1.7.10: rotate(45) then rotate(-90) about Y
            }
            long before = RenderContext.quadCount;
            boolean debug = context == ItemDisplayContext.GUI && (Boolean.getBoolean("legacyconnector.debugItemBounds") || System.getenv("LC_DEBUG") != null);
            if (debug) RenderContext.bounds = new double[] {1e9, 1e9, 1e9, -1e9, -1e9, -1e9};
            handler.renderInventoryBlock(block, view.getItem().getMetadata(view.getItemDamage()), renderId, renderBlocks);
            if (debug) {
                var b = RenderContext.bounds;
                RenderContext.bounds = null;
                if (LOGGED.add("bounds:" + block.getClass().getName())) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").info(
                        "Inventory bounds of {}: x {}..{} y {}..{} z {}..{}", block.getClass().getName(), b[0], b[3], b[1], b[4], b[2], b[5]);
            }
            // AE2: renderInventoryBlock does nothing, its block items are drawn by an IItemRenderer instead.
            return RenderContext.quadCount != before;
        } catch (RuntimeException | LinkageError e) {
            logOnce(handler.getClass().getName(), e);
            return true; // a broken legacy renderer must not take the inventory down
        } finally {
            RenderContext.end();
            pose.popPose();
        }
    }

    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logOnce(String renderer, Throwable e) {
        if (LOGGED.add(renderer)) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Render").error("Legacy item renderer {} failed (logged once)", renderer, e);
    }

    private static boolean helper(IItemRenderer r, ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        try {
            return r.shouldUseRenderHelper(type, stack, helper);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private EntityItem entityItem(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        try {
            if (entityItem == null || entityItemLevel != level) {
                entityItem = new EntityItem(World.mcc$wrap(level));
                entityItemLevel = level;
            }
            entityItem.setEntityItemStack(stack);
            return entityItem;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
