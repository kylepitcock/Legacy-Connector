package org.kylepitcock.legacyconnector.v1_7_10.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * The client places of {@link CoremodSites} that are a NeoForge event on the modern game (generic: the hooks come from the list the loader made of
 * what legacy coremods put into Minecraft's own classes). Nothing is registered without a listed hook.
 */
public final class CoremodClientSites {
    private static boolean started;

    private CoremodClientSites() {}

    public static void register(IEventBus gameBus) {
        if (CoremodSites.Client.MOVEMENT_INPUT.listed()) gameBus.addListener(CoremodClientSites::onMovementInput);
        if (CoremodSites.Client.FOG_COLOR_HOOK.listed()) gameBus.addListener(CoremodClientSites::onFogColor);
        if (CoremodSites.Client.CAMERA_END.listed()) gameBus.addListener(CoremodClientSites::onCameraAngles);
        if (CoremodSites.Client.USE_CLICK.listed()) {
            gameBus.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
                // 1.7.10's rightClickMouse, first thing: true drops the click
                if (e.isUseItem() && CoremodSites.Client.USE_CLICK.active() && CoremodSites.Client.USE_CLICK.cancels()) {
                    e.setSwingHand(false);
                    e.setCanceled(true);
                }
            });
        }
        if (CoremodSites.Client.GAME_STARTED.listed()) {
            gameBus.addListener((ClientTickEvent.Post e) -> {
                // 1.7.10's startGame, last thing: the first tick on which the mods' classes can be called
                if (started || !CoremodSites.Client.GAME_STARTED.active()) return;
                started = true;
                CoremodSites.Client.GAME_STARTED.fire();
            });
        }
        if (CoremodSites.Client.CONTAINER_KEY.listed()) {
            gameBus.addListener((ScreenEvent.KeyPressed.Post e) -> {
                // 1.7.10's keyTyped took the character and the key in one call; a key press has no character yet
                if (e.getScreen() instanceof AbstractContainerScreen<?> && CoremodSites.Client.CONTAINER_KEY.active()) {
                    CoremodSites.Client.CONTAINER_KEY.fire((char) 0, KeyCodes.fromGlfw(e.getKeyCode()));
                }
            });
        }
    }

    private static int handFailures;

    /** {@link CoremodSites.Client#HAND_ITEMS_END}: the legacy drawing code gets the legacy render context at the camera. */
    public static void handItemsEnd(float partialTick, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        if (!CoremodSites.Client.HAND_ITEMS_END.active() || handFailures > 5) return;
        var mc = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft();
        if (mc == null || mc.entityRenderer == null) return;
        RenderContext.beginWorld(pose, buffers, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        try {
            CoremodSites.Client.HAND_ITEMS_END.fire(mc.entityRenderer.itemRenderer, partialTick);
        } catch (RuntimeException | LinkageError e) {
            if (handFailures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Coremods").warn("A coremod hook failed (first person items)", e);
        } finally {
            RenderContext.end();
        }
    }

    private static void onFogColor(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor e) {
        net.minecraft.world.level.Level level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        net.minecraft.world.phys.Vec3 fog = CoremodSites.Client.colorHook(CoremodSites.Client.FOG_COLOR_HOOK, level);
        if (fog == null) return;
        e.setRed((float) fog.x);
        e.setGreen((float) fog.y);
        e.setBlue((float) fog.z);
    }

    private static final org.joml.Vector3f EULER = new org.joml.Vector3f();
    private static int cameraFailures;

    /** {@link CoremodSites.Client#CAMERA_END}: the rotations the hook draws are taken as roll, yaw and pitch of the camera. */
    private static void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        if (!CoremodSites.Client.CAMERA_END.active() || cameraFailures > 5 || e.getCamera().getEntity() != net.minecraft.client.Minecraft.getInstance().player) return;
        com.mojang.blaze3d.vertex.PoseStack scratch = new com.mojang.blaze3d.vertex.PoseStack();
        RenderContext.beginWorld(scratch, null, 0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        try {
            CoremodSites.Client.CAMERA_END.fire((float) e.getPartialTick());
            org.joml.Matrix4f m = scratch.last().pose();
            if (m.m00() == 1.0F && m.m11() == 1.0F && m.m22() == 1.0F && m.m01() == 0.0F && m.m02() == 0.0F && m.m12() == 0.0F) return;
            m.getEulerAnglesZYX(EULER);
            e.setRoll(e.getRoll() + (float) Math.toDegrees(EULER.z));
            e.setYaw(e.getYaw() + (float) Math.toDegrees(EULER.y));
            e.setPitch(e.getPitch() + (float) Math.toDegrees(EULER.x));
        } catch (RuntimeException | LinkageError ex) {
            if (cameraFailures++ < 3) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Coremods").warn("A coremod hook failed (camera orientation)", ex);
        } finally {
            RenderContext.end();
        }
    }

    /** EntityPlayerSP.onLivingUpdate right after the keyboard was read: hooks see (and may change) the player's movement input. */
    private static void onMovementInput(MovementInputUpdateEvent e) {
        if (!(e.getEntity() instanceof LocalPlayer local) || !CoremodSites.Client.MOVEMENT_INPUT.active()) return;
        if (!(EntityBridge.wrap(local) instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.EntityPlayerSP view)) return;
        var in = e.getInput();
        var legacy = view.movementInput;
        legacy.moveForward = in.forwardImpulse;
        legacy.moveStrafe = in.leftImpulse;
        legacy.jump = in.jumping;
        legacy.sneak = in.shiftKeyDown;
        CoremodSites.Client.MOVEMENT_INPUT.fire(view, legacy);
        // a hook that held the player back or took a key away
        if (legacy.moveForward != in.forwardImpulse) in.forwardImpulse = legacy.moveForward;
        if (legacy.moveStrafe != in.leftImpulse) in.leftImpulse = legacy.moveStrafe;
        if (legacy.jump != in.jumping) in.jumping = legacy.jump;
        if (legacy.sneak != in.shiftKeyDown) in.shiftKeyDown = legacy.sneak;
    }
}
