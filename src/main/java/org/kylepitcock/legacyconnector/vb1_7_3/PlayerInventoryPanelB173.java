package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler;

/** The original player's owned equipment slots, transacted beside an unchanged native menu. */
public final class PlayerInventoryPanelB173 {
    private PlayerInventoryPanelB173() {}

    public static int[] slots(ClientPlayerEntity actor) {
        if (actor == null) return new int[0];
        return select(actor.playerScreenHandler, PlayerInventoryStateB173.bindings(PlayerApiB173.bases(actor), actor.inventory));
    }

    static int[] select(ScreenHandler handler, List<PlayerInventoryStateB173.Binding> bindings) {
        if (handler == null || handler.slots.size() > PlayerInventoryStateB173.MAX_SLOTS) return new int[0];
        Set<Inventory> owned = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var binding : bindings) owned.add(binding.inventory());
        var selected = new java.util.ArrayList<Integer>();
        for (int i = 0; i < handler.slots.size(); i++) {
            var slot = handler.slots.get(i);
            if (owned.contains(slot.inventory) && slot.index >= 0 && slot.index < slot.inventory.size()) selected.add(i);
        }
        return selected.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Server-only; the network layer additionally validates its native-screen epoch and revision. */
    public static void click(PlayerViewB173 actor, int slot, int button, boolean quick) {
        if (actor == null || !(actor.mcc$modern instanceof net.minecraft.server.level.ServerPlayer player)
                || button < 0 || button > 1 || Arrays.stream(slots(actor)).noneMatch(index -> index == slot)) return;
        ScreenHandler handler = actor.playerScreenHandler;
        if (handler == null || !handler.canUse(actor)) return;
        var nativeMenu = player.inventoryMenu;
        var carried = nativeMenu.getCarried();
        // An item unknown to Beta cannot be placed; retain the native cursor without conversion loss.
        if (!carried.isEmpty() && !StacksB173.canRepresent(carried)) return;
        actor.inventory.setCursorStack(StacksB173.toLegacy(carried));
        try {
            PlayerApiB173.withPlayer(actor, () -> handler.onSlotClick(slot, button, quick, actor));
        } finally {
            nativeMenu.setCarried(StacksB173.toModern(actor.inventory.getCursorStack()));
            actor.writeBack();
        }
    }
}
