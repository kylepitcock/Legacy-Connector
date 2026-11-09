package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;

/** Original inventory GUI requests offered by a hook, retained for an explicit native tab action. */
public final class PlayerMenuContextB173 {
    private PlayerMenuContextB173() {}

    public static boolean offer(PlayerEntity actor, Screen screen) {
        if (!(screen instanceof HandledScreen gui) || !matches(actor, gui)) return false;
        boolean installed = ContentB173.loadedMods().stream()
                .anyMatch(mod -> RemoteMenuContextB173.owned(gui.getClass(), mod.getClass()));
        if (!installed) return false;
        actor.mcc$inventoryScreen = gui;
        return true;
    }

    static boolean matches(PlayerEntity actor, HandledScreen gui) {
        return actor != null && actor.playerScreenHandler != null
                && gui != null && gui.container == actor.playerScreenHandler;
    }

    public static HandledScreen candidate(PlayerEntity actor) {
        if (actor == null) return null;
        HandledScreen gui = actor.mcc$inventoryScreen;
        if (!matches(actor, gui)) { actor.mcc$inventoryScreen = null; return null; }
        return gui;
    }

    public static ScreenHandler container(PlayerEntity actor) {
        HandledScreen gui = candidate(actor);
        return gui == null ? null : gui.container;
    }

    static boolean isPlayerMenu(ScreenHandler handler) {
        if (handler == null) return false;
        for (var slot : handler.slots)
            if (slot.inventory instanceof legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory inventory
                    && inventory.player != null && inventory.player.playerScreenHandler == handler) return true;
        return false;
    }

    /** Only an already selected installed GUI class is considered; never scans constructors of other classes. */
    static HandledScreen construct(Class<? extends HandledScreen> type, PlayerEntity player, int[][] layout)
            throws ReflectiveOperationException {
        if (player instanceof ClientPlayerEntity client) PlayerApiB173.ensure(client);
        if (player == null || player.playerScreenHandler == null) return null;
        java.lang.reflect.Constructor<? extends HandledScreen> constructor;
        try { constructor = type.getConstructor(PlayerEntity.class); }
        catch (NoSuchMethodException missing) { return null; }
        HandledScreen gui = constructor.newInstance(player);
        return matches(player, gui) && RemoteMenuContextB173.matches(gui.container, layout) ? gui : null;
    }
}
