package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingResultInventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler;
import legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class PlayerInventoryMenuB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        VanillaB173.init();
    }
    private static PlayerEntity player() { return new PlayerEntity(null) {}; }

    @Test void vanillaLayoutAndArmorPredicatesSurviveTheNativeStorageAdapter() {
        int[] mark = ContentB173.mark();
        try {
            var player = player();
            var handler = new PlayerScreenHandler(player.inventory);
            assertEquals(45, handler.slots.size());
            var helmet = new ItemStack(new ArmorItem(30120, 0, 0, 0));
            var boots = new ItemStack(new ArmorItem(30121, 0, 0, 3));
            var head = handler.slots.get(5);
            assertEquals(39, head.index);
            assertEquals(1, head.getMaxItemCount());
            assertTrue(head.canInsert(helmet));
            assertFalse(head.canInsert(boots));
            assertFalse(head.canInsert(null));
            // PlayerSlot's backing inventory is the native player's, rather than a legacy adapter.
            var modernInventory = new net.minecraft.world.entity.player.Inventory(null);
            var nativeHead = new LegacyB173Menu.PlayerSlot(modernInventory, head);
            assertSame(modernInventory, nativeHead.container);
            assertEquals(1, nativeHead.getMaxStackSize());
            assertFalse(nativeHead.mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)));
            assertTrue(new LegacyB173Menu.PlayerSlot(modernInventory, handler.slots.get(9))
                    .mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)));
        } finally { ContentB173.release(mark); }
    }

    static class ExtendedHandler extends PlayerScreenHandler {
        final CraftingResultInventory extra = new CraftingResultInventory();
        ExtendedHandler(PlayerEntity player) {
            super(player.inventory);
            addSlot(new Slot(extra, 0, 80, 8) {
                @Override public int getMaxItemCount() { return 1; }
                @Override public boolean canInsert(ItemStack stack) { return false; }
            });
        }
    }

    @Test void inheritedShiftClickMovesExtraToPlayerAndNeverAutoEquipsExtraSlots() {
        int[] mark = ContentB173.mark();
        try {
            var player = player();
            var handler = new ExtendedHandler(player);
            var item = new Item(30122) {};
            handler.extra.setStack(0, new ItemStack(item));
            assertNotNull(handler.quickMove(45));
            assertNull(handler.extra.getStack(0));
            assertEquals(item.id, player.inventory.getStack(9).itemId);
            assertNotNull(handler.quickMove(9));
            assertNull(player.inventory.getStack(9));
            assertEquals(item.id, player.inventory.getStack(0).itemId);
            assertNull(handler.extra.getStack(0));
        } finally { ContentB173.release(mark); }
    }

    @Test void originalCraftingPickupConsumesInputsAndClosingDropsOnlyTheRemainingGrid() {
        int[] mark = ContentB173.mark();
        try {
            var dropped = new java.util.ArrayList<ItemStack>();
            var player = new PlayerEntity(null) {
                @Override public void dropItem(ItemStack stack) { dropped.add(stack); }
            };
            var handler = new PlayerScreenHandler(player.inventory);
            var item = new Item(30123) {};
            handler.craftingInput.setStack(0, new ItemStack(item, 3));
            handler.craftingInput.setStack(3, new ItemStack(item, 1));
            handler.slots.getFirst().onTakeItem(null);
            assertEquals(2, handler.craftingInput.getStack(0).count);
            assertNull(handler.craftingInput.getStack(3));
            handler.onClosed(player);
            assertEquals(1, dropped.size());
            assertEquals(2, dropped.getFirst().count);
            for (int i = 0; i < 4; i++) assertNull(handler.craftingInput.getStack(i));
            handler.onClosed(player);
            assertEquals(1, dropped.size());
        } finally { ContentB173.release(mark); }
    }

    public static class PlayerGui extends HandledScreen {
        public PlayerGui(PlayerEntity player) { super(player.playerScreenHandler); }
        @Override public void drawBackground(float partial) {}
    }

    @Test void playerConstructorReconstructionUsesCanonicalHandlerAndChecksLayout() throws Exception {
        var player = player();
        player.playerScreenHandler = new ExtendedHandler(player);
        var handler = player.playerScreenHandler;
        int[][] layout = handler.slots.stream().map(s -> new int[] {
                LegacyB173Menu.isPlayerSlot(s) ? 0 : 1, s.index, s.x, s.y }).toArray(int[][]::new);
        var context = RemoteMenuContextB173.capture(handler);
        assertTrue(context.getBoolean("player"));
        var gui = RemoteMenuContextB173.construct(PlayerGui.class, context, player.inventory, null, layout);
        assertNotNull(gui);
        assertSame(handler, gui.container);
        layout[45][2]++;
        assertNull(RemoteMenuContextB173.construct(PlayerGui.class, context, player.inventory, null, layout));
        player.mcc$inventoryScreen = gui;
        assertSame(gui, PlayerMenuContextB173.candidate(player));
        player.playerScreenHandler = new PlayerScreenHandler(player.inventory);
        assertNull(PlayerMenuContextB173.candidate(player));
    }
}
