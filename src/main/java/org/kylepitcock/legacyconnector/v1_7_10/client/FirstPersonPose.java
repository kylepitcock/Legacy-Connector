package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 1.7.10's first-person item matrix, for {@code IItemRenderer}s drawing {@code EQUIPPED_FIRST_PERSON}: they are tuned to it
 * (Thaumcraft's Thaumometer undoes it to hold the device in front of the camera) and land off screen from the modern item
 * display transform. The main hand's pose is captured where NeoForge posts {@link RenderHandEvent} (camera bob and arm sway
 * applied, as in 1.7.10 before {@code renderItemInFirstPerson} placed the item), and the item is placed from it the 1.7.10 way.
 */
public final class FirstPersonPose {
    private static final Matrix4f POSE = new Matrix4f();
    private static final Matrix3f NORMAL = new Matrix3f();
    private static float equipProgress = 1.0F;
    private static float swing;
    private static long frame = -1;

    private FirstPersonPose() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            if (e.getHand() != InteractionHand.MAIN_HAND) return;
            POSE.set(e.getPoseStack().last().pose());
            NORMAL.set(e.getPoseStack().last().normal());
            equipProgress = 1.0F - e.getEquipProgress(); // modern 0 = raised, 1.7.10 1 = raised
            swing = e.getSwingProgress();
            frame = net.minecraft.client.Minecraft.getInstance().getFrameTimeNs();
        });
    }

    /** How far the held item is raised, 1.7.10's {@code equippedProgress}. */
    public static float equipProgress() {
        return equipProgress;
    }

    /**
     * Replaces the pose with 1.7.10's first-person item matrix, through Forge's {@code renderEquippedItem}. False when this
     * frame's hand pose is unknown (the caller keeps the modern transform).
     */
    public static boolean apply(PoseStack pose, boolean block3d) {
        if (frame != net.minecraft.client.Minecraft.getInstance().getFrameTimeNs()) return false;
        pose.last().pose().set(POSE);
        pose.last().normal().set(NORMAL);
        float f6 = Mth.sin(swing * Mth.PI);
        float f7 = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        // ItemRenderer.renderItemInFirstPerson (not using an item)
        pose.translate(-f7 * 0.4F, Mth.sin(Mth.sqrt(swing) * Mth.PI * 2.0F) * 0.2F, -f6 * 0.2F);
        pose.translate(0.7F * 0.8F, -0.65F * 0.8F - (1.0F - equipProgress) * 0.6F, -0.9F * 0.8F);
        pose.mulPose(Axis.YP.rotationDegrees(45.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.sin(swing * swing * Mth.PI) * 20.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-f7 * 20.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-f7 * 80.0F));
        pose.scale(0.4F, 0.4F, 0.4F);
        // ForgeHooksClient.renderEquippedItem
        if (block3d) {
            pose.translate(-0.5F, -0.5F, -0.5F);
        } else {
            pose.translate(0.0F, -0.3F, 0.0F);
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.mulPose(Axis.YP.rotationDegrees(50.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(335.0F));
            pose.translate(-0.9375F, -0.0625F, 0.0F);
        }
        return true;
    }
}
