package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerInventory;
import legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

/** Validated coordinate proposals only: native storage, indices, slots and positions are never changed here. */
public final class PlayerInventoryLayoutB173 {
    public record Position(Slot slot, int x, int y) {}
    private static final int MAX_EXTENT = 4096;
    private PlayerInventoryLayoutB173() {}

    public static List<Position> nativeSlots(InventoryMenu menu, ClientPlayerEntity actor) {
        if (menu == null || actor == null || !(actor.playerScreenHandler instanceof PlayerScreenHandler canonical)
                || actor.inventory == null || actor.inventory.player != actor
                || !(actor.mcc$modern instanceof net.minecraft.world.entity.player.Player player)
                || player.inventoryMenu != menu || menu.slots.isEmpty()) return List.of();
        int width = MAX_EXTENT, height = MAX_EXTENT;
        var gui = actor.mcc$inventoryScreen;
        if (gui != null && gui.container == canonical) {
            width = gui.mcc$backgroundWidth();height = gui.mcc$backgroundHeight();
        }
        var result = menu.getSlot(0);
        if (!(result instanceof net.minecraft.world.inventory.ResultSlot) || result.getContainerSlot() != 0) return List.of();
        return map(menu.slots, player.getInventory(), menu.getCraftSlots(), result.container,
                actor.inventory, canonical, width, height);
    }

    /** Structural entrypoint permits regression fixtures made solely from inventory/slot primitives. */
    static List<Position> map(List<Slot> nativeSlots, Container playerInventory, Container crafting, Container result,
            PlayerInventory legacyInventory, PlayerScreenHandler canonical, int width, int height) {
        if (nativeSlots == null || nativeSlots.size() > PlayerInventoryStateB173.MAX_SLOTS || canonical == null
                || canonical.slots.size() > PlayerInventoryStateB173.MAX_SLOTS || playerInventory == null || crafting == null || result == null
                || playerInventory == crafting || playerInventory == result || crafting == result || legacyInventory == null
                || canonical.craftingInput == null || canonical.craftingResult == null
                || canonical.craftingResult == legacyInventory
                || canonical.craftingInput == canonical.craftingResult
                || width < 16 || height < 16 || width > MAX_EXTENT || height > MAX_EXTENT) return List.of();
        var originals = new legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot[45];
        var rectangles = new ArrayList<legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot>();
        for (var slot : canonical.slots) {
            if (slot == null || slot.inventory == null || slot.x < 0 || slot.y < 0 || slot.x > width - 16 || slot.y > height - 16) return List.of();
            for (var previous : rectangles)
                if (slot.x < previous.x + 16 && slot.x + 16 > previous.x
                        && slot.y < previous.y + 16 && slot.y + 16 > previous.y) return List.of();
            rectangles.add(slot);
            int key = slot.inventory == legacyInventory && slot.index >= 0 && slot.index < 40 ? slot.index
                    : slot.inventory == canonical.craftingInput && slot.index >= 0 && slot.index < 4 ? 40 + slot.index
                    : slot.inventory == canonical.craftingResult && slot.index == 0 ? 44 : -1;
            if (key >= 0) {
                if (originals[key] != null) return List.of();
                originals[key] = slot;
            }
        }
        for (var slot : originals) if (slot == null) return List.of();
        var proposed = new ArrayList<Position>(45);
        var seen = new boolean[45];
        for (Slot slot : nativeSlots) {
            if (slot == null) return List.of();
            int index = slot.getContainerSlot();
            int key = slot.container == playerInventory && index >= 0 && index < 40 ? index
                    : slot.container == crafting && index >= 0 && index < 4 ? 40 + index
                    : slot.container == result && index == 0 ? 44 : -1;
            if (key < 0) {
                // The modern offhand is relocated separately. Keep another mod's in-panel slot presentation intact.
                if (!(slot.container == playerInventory && index == 40)
                        && slot.x < width && slot.x + 16 > 0 && slot.y < height && slot.y + 16 > 0) return List.of();
                continue;
            }
            if (seen[key]) return List.of();
            seen[key] = true;
            var original = originals[key];
            proposed.add(new Position(slot, original.x, original.y));
        }
        if (proposed.size() != 45) return List.of();
        return List.copyOf(proposed);
    }
}
