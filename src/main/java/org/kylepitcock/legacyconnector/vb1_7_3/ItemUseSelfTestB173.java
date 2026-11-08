package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Deterministic interaction regression checks, run only by the development server self test. */
final class ItemUseSelfTestB173 {
    private static ProbeItem legacy;
    private static LegacyB173Item host;

    static void register() {
        if (host != null) return;
        int[] mark = ContentB173.mark();
        try {
            int id = legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS.length - 1;
            while (legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[id] != null) id--;
            legacy = new ProbeItem(id - 256);
            host = new LegacyB173Item(legacy, new net.minecraft.world.item.Item.Properties());
            org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "b173_item_use_test"), host);
        } finally {
            ContentB173.release(mark);
        }
    }

    static void run(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (host == null) return;
        try {
            player.getInventory().clearContent();
            player.getInventory().selected = 2;
            player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(Items.COAL, 4));
            player.getInventory().items.set(8, new net.minecraft.world.item.ItemStack(Items.NETHERITE_INGOT));
            legacy.mode = 0;
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
            require(host.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "block use result");
            require(legacy.side == 5 && legacy.pos.equals(pos) && legacy.world.mcc$level() == level, "block context");
            require(player.getMainHandItem().getCount() == 3 && player.getInventory().items.get(8).is(Items.NETHERITE_INGOT), "consume and preserve unrelated item");
            legacy.mode = 1;
            host.use(level, player, InteractionHand.MAIN_HAND);
            require(player.getMainHandItem().is(Items.DIAMOND), "returned replacement stack");
            legacy.mode = 2;
            host.use(level, player, InteractionHand.MAIN_HAND);
            require(player.getMainHandItem().isEmpty(), "returned null consumes stack");
            player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(Items.COAL));
            legacy.mode = 3;
            host.use(level, player, InteractionHand.MAIN_HAND);
            require(player.getMainHandItem().is(Items.STICK), "direct held-inventory replacement");
            legacy.mode = 4;
            host.use(level, player, InteractionHand.MAIN_HAND);
            require(player.containerMenu instanceof LegacyB173Menu && player.containerMenu.slots.size() == 63, "item opens real server menu");
            player.closeContainer();
            int calls = legacy.calls;
            require(host.use(level, player, InteractionHand.OFF_HAND).getResult() == InteractionResult.PASS && legacy.calls == calls, "offhand does not duplicate beta use");
            legacy.mode = 5;
            require(host.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.PASS, "unhandled block use passes");
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 ITEM USE TEST: all interaction and item GUI checks passed");
        } finally {
            player.closeContainer();
            player.getInventory().clearContent();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("B1.7.3 ITEM USE TEST failed: " + message);
    }

    private static final class ProbeItem extends legacyconnector.vb1_7_3.net.minecraft.item.Item {
        int mode, calls, side;
        BlockPos pos;
        World world;

        ProbeItem(int id) { super(id); }

        @Override
        public boolean useOnBlock(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side) {
            calls++;
            this.world = world;
            this.pos = new BlockPos(x, y, z);
            this.side = side;
            if (mode == 5) return false;
            stack.count--;
            return true;
        }

        @Override
        public ItemStack use(ItemStack stack, World world, PlayerEntity player) {
            calls++;
            if (mode == 1) return new ItemStack(264, 1, 0);
            if (mode == 2) return null;
            if (mode == 3) player.inventory.main[player.inventory.selectedSlot] = new ItemStack(280, 1, 0);
            if (mode == 4) legacyconnector.vb1_7_3.net.minecraft.src.ModLoader.OpenGUI(player,
                    new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.DoubleChestScreen(player.inventory,
                            new legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity()));
            return stack;
        }
    }
}
