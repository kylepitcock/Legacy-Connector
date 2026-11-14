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
