package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.*;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import org.junit.jupiter.api.Test;

class PlayerInventoryStateB173Test {
    static class OwnedInventory implements Inventory {
        final int size;
        OwnedInventory(int size) { this.size = size; }
        public int size() { return size; }
        public ItemStack getStack(int slot) { return null; }
        public ItemStack removeStack(int slot, int count) { return null; }
        public void setStack(int slot, ItemStack stack) {}
        public String getName() { return "fixture"; }
        public int getMaxCountPerStack() { return 1; }
        public void markDirty() {}
        public boolean canPlayerUse(PlayerEntity player) { return true; }
    }
    static class Parent extends PlayerBase {
        private Inventory inherited = new OwnedInventory(2);
        Parent() { super(null); }
    }
    static class Extension extends Parent {
        static Inventory global = new OwnedInventory(4);
        private Inventory owned = new OwnedInventory(8);
        private Inventory alias = owned;
        private Inventory crafting = new CraftingInventory(null, 2, 2);
        private Inventory result = new CraftingResultInventory();
        private Inventory nativeInventory;
        private Object untyped = new OwnedInventory(3);
    }

    @Test void discoversOnlyDirectTypedInstanceOwnershipAndKeepsInheritedBindingsStable() {
        var actor = new ClientPlayerEntity(null) {};
        var extension = new Extension();
        extension.nativeInventory = actor.inventory;
        var bindings = PlayerInventoryStateB173.bindings(List.of(extension), actor.inventory);
        assertEquals(2, bindings.size());
        assertSame(extension.owned, bindings.get(0).inventory());
        assertEquals(Extension.class.getName() + "#" + Extension.class.getName() + "#alias", bindings.get(0).key());
        assertEquals(Extension.class.getName() + "#" + Parent.class.getName() + "#inherited", bindings.get(1).key());
        assertEquals(bindings, PlayerInventoryStateB173.bindings(List.of(extension), actor.inventory));
    }

    @Test void rejectsOversizedAndAggregateInventoryOwnership() {
        var extension = new Extension();
        extension.owned = new OwnedInventory(513);
        extension.alias = extension.owned;
        assertThrows(IllegalStateException.class, () -> PlayerInventoryStateB173.bindings(List.of(extension), null));
        extension.owned = new OwnedInventory(511);
        extension.alias = extension.owned;
        assertThrows(IllegalStateException.class, () -> PlayerInventoryStateB173.bindings(List.of(extension), null));
    }
}
