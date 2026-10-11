package org.kylepitcock.mccompat.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mccompat.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import mccompat.v1_7_10.net.minecraft.entity.item.EntityItem;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.world.World;
import mccompat.v1_7_10.net.minecraftforge.client.IItemRenderer;
import mccompat.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType;
import mccompat.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRendererHelper;
import mccompat.v1_7_10.net.minecraftforge.client.MinecraftForgeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;

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
        if (renderer == null && type != ItemRenderType.ENTITY) {
            type = ItemRenderType.ENTITY; // e.g. renderers that only draw dropped/held forms
            renderer = MinecraftForgeClient.getItemRenderer(view, type);
        }
        if (renderer == null) return;
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
                        pose.mulPose(Axis.YP.rotationDegrees(225));
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
                    data = new Object[] {renderBlocks, Minecraft.getInstance().player == null ? null : EntityBridge.wrap(Minecraft.getInstance().player)};
                }
            }
            renderer.renderItem(type, view, data);
        } catch (RuntimeException | LinkageError e) {
            // a broken legacy renderer must not take the inventory down
        } finally {
            RenderContext.end();
            pose.popPose();
        }
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
