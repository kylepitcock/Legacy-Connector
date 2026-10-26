package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.7.10's first-person/held item renderer: mods' entity renderers call it to draw the item an entity holds. */
public class ItemRenderer {
    /**
     * How far the held item is raised (1 = fully up). Mods read these by reflection to place first-person models (Thaumcraft's
     * Thaumometer); missing, they read 0 and drew the item 1.5 blocks below the screen.
     */
    private float equippedProgress = 1.0F;
    private float prevEquippedProgress = 1.0F;

    public void mcc$setEquipped(float progress) {
        equippedProgress = prevEquippedProgress = progress;
    }

    public ItemRenderer() {
    }

    /** Mods make their own to draw items into GUIs and models (the Minecraft is the one client either way). */
    public ItemRenderer(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc) {
    }

    /** Starts the re-equip animation (the held item drops and comes back up), as the modern hand renderer does after use. */
    public void resetEquippedProgress() {
        equippedProgress = 0.0F;
        var mc = Minecraft.getInstance();
        if (mc.gameRenderer != null && mc.player != null) mc.gameRenderer.itemInHandRenderer.itemUsed(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public void resetEquippedProgress2() {
        resetEquippedProgress();
    }

    /** The modern hand renderer ticks the equip animation itself. */
    public void updateEquippedItem() {
    }

    /**
     * Fire, in-wall and underwater screen overlays. The modern game draws them after the hands whether or not a mod
     * replaced the hand rendering (RenderHandEvent), so drawing them here would draw them twice.
     */
    public void renderOverlays(float partialTicks) {
    }

    private static java.lang.reflect.Method mcc$renderArm;
    private static java.lang.reflect.Field mcc$mainItem, mcc$height, mcc$oHeight;
    private static boolean mcc$reflected;
    private static boolean mcc$inFirstPerson;

    private static synchronized void mcc$reflect() {
        if (mcc$reflected) return;
        mcc$reflected = true;
        try {
            Class<?> c = net.minecraft.client.renderer.ItemInHandRenderer.class;
            mcc$renderArm = c.getDeclaredMethod("renderArmWithItem", net.minecraft.client.player.AbstractClientPlayer.class, float.class, float.class,
                    net.minecraft.world.InteractionHand.class, float.class, net.minecraft.world.item.ItemStack.class, float.class,
                    com.mojang.blaze3d.vertex.PoseStack.class, net.minecraft.client.renderer.MultiBufferSource.class, int.class);
            mcc$renderArm.setAccessible(true);
            mcc$mainItem = c.getDeclaredField("mainHandItem");
            mcc$mainItem.setAccessible(true);
            mcc$height = c.getDeclaredField("mainHandHeight");
            mcc$height.setAccessible(true);
            mcc$oHeight = c.getDeclaredField("oMainHandHeight");
            mcc$oHeight.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            mcc$renderArm = null;
        }
    }

    /**
     * Draws the local player's main hand (the held item, or the bare arm) in the current pose, as the modern hand renderer
     * places it. Mods replacing the hand rendering call this after their own transforms (Witchery's transformed players);
     * it does not post RenderHandEvent again, as 1.7.10 did not.
     */
    public void renderItemInFirstPerson(float partialTicks) {
        var pose = RenderContext.pose();
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (pose == null || player == null || mc.gameRenderer == null || mcc$inFirstPerson) return;
        mcc$reflect();
        var hand = mc.gameRenderer.itemInHandRenderer;
        int light = mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTicks);
        float pitch = net.minecraft.util.Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        float swing = player.swingingArm == null || player.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getAttackAnim(partialTicks) : 0.0F;
        mcc$inFirstPerson = true;
        try {
            if (mcc$renderArm != null) {
                var stack = (net.minecraft.world.item.ItemStack) mcc$mainItem.get(hand);
                float equip = 1.0F - net.minecraft.util.Mth.lerp(partialTicks, mcc$oHeight.getFloat(hand), mcc$height.getFloat(hand));
                mcc$renderArm.invoke(hand, player, partialTicks, pitch, net.minecraft.world.InteractionHand.MAIN_HAND, swing, stack, equip, pose,
                        RenderContext.buffers(), light);
            } else {
                hand.renderItem(player, player.getMainHandItem(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, pose, RenderContext.buffers(), light);
            }
        } catch (ReflectiveOperationException e) {
            // nothing drawn this frame
        } finally {
            mcc$inFirstPerson = false;
        }
    }

    public void renderItem(EntityLivingBase entity, ItemStack stack, int pass) {
        var pose = RenderContext.pose();
        var modern = ItemStack.mcc$toModern(stack);
        if (pose == null || modern == null || modern.isEmpty()) return;
        var mc = Minecraft.getInstance();
        LivingEntity living = entity != null && entity.mcc$entity instanceof LivingEntity l ? l : null;
        mc.getItemRenderer().renderStatic(living, modern, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, RenderContext.buffers(), (net.minecraft.world.level.Level) (Object) mc.level,
                RenderContext.light(), RenderContext.overlay(), 0);
    }

    /**
     * Draws an item icon as a flat sprite with thickness: the front and back faces, and one strip per pixel along each edge
     * (the thick look of held tools). {@code u1..v2} are the icon's atlas coordinates, {@code width}/{@code height} its pixels.
     */
    public static void renderItemIn2D(Tessellator t, float u1, float v1, float u2, float v2, int width, int height, float thickness) {
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(0.0D, 0.0D, 0.0D, u1, v2);
        t.addVertexWithUV(1.0D, 0.0D, 0.0D, u2, v2);
        t.addVertexWithUV(1.0D, 1.0D, 0.0D, u2, v1);
        t.addVertexWithUV(0.0D, 1.0D, 0.0D, u1, v1);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, -1.0F);
        t.addVertexWithUV(0.0D, 1.0D, -thickness, u1, v1);
        t.addVertexWithUV(1.0D, 1.0D, -thickness, u2, v1);
        t.addVertexWithUV(1.0D, 0.0D, -thickness, u2, v2);
        t.addVertexWithUV(0.0D, 0.0D, -thickness, u1, v2);
        t.draw();
        float halfU = 0.5F * (u1 - u2) / width;
        float halfV = 0.5F * (v2 - v1) / height;
        t.startDrawingQuads();
        t.setNormal(-1.0F, 0.0F, 0.0F);
        for (int k = 0; k < width; ++k) {
            float x = (float) k / width;
            float u = u1 + (u2 - u1) * x - halfU;
            t.addVertexWithUV(x, 0.0D, -thickness, u, v2);
            t.addVertexWithUV(x, 0.0D, 0.0D, u, v2);
            t.addVertexWithUV(x, 1.0D, 0.0D, u, v1);
            t.addVertexWithUV(x, 1.0D, -thickness, u, v1);
        }
        t.draw();
        t.startDrawingQuads();
        t.setNormal(1.0F, 0.0F, 0.0F);
        for (int k = 0; k < width; ++k) {
            float x = (float) k / width;
            float u = u1 + (u2 - u1) * x - halfU;
            float x2 = x + 1.0F / width;
            t.addVertexWithUV(x2, 1.0D, -thickness, u, v1);
            t.addVertexWithUV(x2, 1.0D, 0.0D, u, v1);
            t.addVertexWithUV(x2, 0.0D, 0.0D, u, v2);
            t.addVertexWithUV(x2, 0.0D, -thickness, u, v2);
        }
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 1.0F, 0.0F);
        for (int k = 0; k < height; ++k) {
            float y = (float) k / height;
            float v = v2 + (v1 - v2) * y - halfV;
            float y2 = y + 1.0F / height;
            t.addVertexWithUV(0.0D, y2, 0.0D, u1, v);
            t.addVertexWithUV(1.0D, y2, 0.0D, u2, v);
            t.addVertexWithUV(1.0D, y2, -thickness, u2, v);
            t.addVertexWithUV(0.0D, y2, -thickness, u1, v);
        }
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, -1.0F, 0.0F);
        for (int k = 0; k < height; ++k) {
            float y = (float) k / height;
            float v = v2 + (v1 - v2) * y - halfV;
            t.addVertexWithUV(1.0D, y, 0.0D, u2, v);
            t.addVertexWithUV(0.0D, y, 0.0D, u1, v);
            t.addVertexWithUV(0.0D, y, -thickness, u1, v);
            t.addVertexWithUV(1.0D, y, -thickness, u2, v);
        }
        t.draw();
    }

    public void renderItem(EntityLivingBase entity, ItemStack stack, int pass, legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType type) {
        renderItem(entity, stack, pass);
    }
}
