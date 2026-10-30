package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2 first-person hand pieces ({@code ItemRenderer.renderArmFirstPerson}, {@code renderArms}, the held stacks) drawn by the modern
 * hand renderer's own private methods, at the legacy matrix of the current draw. Missing methods (another mapping) draw nothing.
 */
public final class Hands1122 {
    private static Method renderPlayerArm, renderMapHand;
    private static Field mainHandItem, offHandItem;
    private static boolean reflected;

    private Hands1122() {
    }

    private static synchronized void reflect() {
        if (reflected) return;
        reflected = true;
        Class<?> c = ItemInHandRenderer.class;
        try {
            renderPlayerArm = c.getDeclaredMethod("renderPlayerArm", com.mojang.blaze3d.vertex.PoseStack.class, net.minecraft.client.renderer.MultiBufferSource.class,
                    int.class, float.class, float.class, HumanoidArm.class);
            renderPlayerArm.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            renderPlayerArm = null;
        }
        try {
            renderMapHand = c.getDeclaredMethod("renderMapHand", com.mojang.blaze3d.vertex.PoseStack.class, net.minecraft.client.renderer.MultiBufferSource.class,
                    int.class, HumanoidArm.class);
            renderMapHand.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            renderMapHand = null;
        }
        try {
            mainHandItem = c.getDeclaredField("mainHandItem");
            mainHandItem.setAccessible(true);
            offHandItem = c.getDeclaredField("offHandItem");
            offHandItem.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            mainHandItem = offHandItem = null;
        }
    }

    private static int light() {
        var mc = Minecraft.getInstance();
        return mc.player == null ? RenderContext.light() : mc.getEntityRenderDispatcher().getPackedLightCoords(mc.player, mc.getTimer().getGameTimeDeltaPartialTick(true));
    }

    /** The bare arm of the local player on one side, raised by {@code equipProgress} and swinging by {@code swingProgress}. */
    public static void renderArm(float equipProgress, float swingProgress, HumanoidArm side) {
        var pose = RenderContext.pose();
        var mc = Minecraft.getInstance();
        if (pose == null || mc.player == null || mc.gameRenderer == null || side == null) return;
        reflect();
        if (renderPlayerArm == null) return;
        try {
            renderPlayerArm.invoke(mc.gameRenderer.itemInHandRenderer, pose, RenderContext.buffers(), light(), equipProgress, swingProgress, side);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RenderErrors1122.report("drawing the first-person arm", e);
        }
    }

    /** Both arms holding a map (1.12's {@code renderArms}): turned to face the map, unless the player is invisible. */
    public static void renderMapArms() {
        var pose = RenderContext.pose();
        var mc = Minecraft.getInstance();
        if (pose == null || mc.player == null || mc.gameRenderer == null || mc.player.isInvisible()) return;
        reflect();
        if (renderMapHand == null) return;
        pose.pushPose();
        try {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            int light = light();
            renderMapHand.invoke(mc.gameRenderer.itemInHandRenderer, pose, RenderContext.buffers(), light, HumanoidArm.RIGHT);
            renderMapHand.invoke(mc.gameRenderer.itemInHandRenderer, pose, RenderContext.buffers(), light, HumanoidArm.LEFT);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RenderErrors1122.report("drawing the map arms", e);
        } finally {
            pose.popPose();
        }
    }

    /** The stack the hand renderer shows in a hand (it lags behind the inventory while the equip animation runs). */
    public static net.minecraft.world.item.ItemStack shownStack(boolean offHand) {
        var mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return net.minecraft.world.item.ItemStack.EMPTY;
        reflect();
        Field f = offHand ? offHandItem : mainHandItem;
        try {
            if (f != null && f.get(mc.gameRenderer.itemInHandRenderer) instanceof net.minecraft.world.item.ItemStack s) return s;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // fall back to the inventory
        }
        if (mc.player == null) return net.minecraft.world.item.ItemStack.EMPTY;
        return offHand ? mc.player.getOffhandItem() : mc.player.getMainHandItem();
    }
}
