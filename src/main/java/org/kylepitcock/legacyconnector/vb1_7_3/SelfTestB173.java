package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development check ({@code -Dlegacyconnector.entitySelfTest=true}): every b1.7.3 mod block is placed in the overworld high above spawn,
 * read back and asked for its drops; logs one summary line ("B1.7.3 SELF TEST") and the blocks that failed.
 */
public final class SelfTestB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");

    private SelfTestB173() {}

    public static void register(IEventBus game) {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        game.addListener((ServerStartedEvent e) -> run(e.getServer().overworld()));
    }

    /**
     * Every legacy callback of every block once, as play would call them: placed (onBlockAdded), a neighbour change, a scheduled and a
     * random tick, a right click by a fake player, the redstone queries and removal (onBlockRemoval). Failures are counted by the blocks.
     */
    private static void behaviour(ServerLevel level, List<LegacyB173Block> blocks, BlockPos base) {
        int before = LegacyB173Block.CALLBACK_FAILURES.get();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var random = level.getRandom();
        int n = 0, used = 0, powering = 0, thrown = 0;
        for (LegacyB173Block b : blocks) {
            BlockPos pos = base.offset((n % 32) * 2, 0, (n / 32) * 2);
            n++;
            try {
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos, b.defaultBlockState(), 3);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() != b) continue;
                state.handleNeighborChanged(level, pos, Blocks.STONE, pos.below(), false);
                state = level.getBlockState(pos);
                if (state.getBlock() == b) state.tick(level, pos, random);
                state = level.getBlockState(pos);
                if (state.getBlock() == b) state.randomTick(level, pos, random);
                state = level.getBlockState(pos);
                if (state.getBlock() == b) {
                    player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
                    if (state.useWithoutItem(level, player, hit).consumesAction()) used++;
                }
                state = level.getBlockState(pos);
                for (var dir : net.minecraft.core.Direction.values()) {
                    if (state.getSignal(level, pos, dir) > 0) {
                        powering++;
                        break;
                    }
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
            } catch (RuntimeException | LinkageError e) {
                thrown++;
                LOG.info("  behaviour of {} threw {}", BuiltInRegistries.BLOCK.getKey(b), e.toString());
            }
        }
        LOG.info("B1.7.3 BEHAVIOUR TEST: {} blocks; {} legacy callbacks failed, {} escaped; {} used a right click; {} emit redstone",
                blocks.size(), LegacyB173Block.CALLBACK_FAILURES.get() - before, thrown, used, powering);
        tileEntities(level, blocks, base.above(4));
    }

    /** Every block with a tile entity: it has one once placed, and the same class comes back from its saved NBT. */
    private static void tileEntities(ServerLevel level, List<LegacyB173Block> blocks, BlockPos base) {
        int withEntity = 0, made = 0, reloaded = 0;
        List<String> failed = new ArrayList<>();
        var world = legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level);
        int n = 0;
        for (LegacyB173Block b : blocks) {
            if (!(b instanceof LegacyB173EntityBlock)) continue;
            withEntity++;
            BlockPos pos = base.offset((n % 32) * 2, 0, (n / 32) * 2);
            n++;
            try {
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos, b.defaultBlockState(), 3);
                var te = world.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
                if (te == null) {
                    failed.add(BuiltInRegistries.BLOCK.getKey(b) + " (no tile entity)");
                    continue;
                }
                made++;
                var host = level.getBlockEntity(pos);
                var tag = host.saveWithFullMetadata(level.registryAccess());
                var copy = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
                if (copy instanceof LegacyB173BlockEntity h && h.legacy() != null && h.legacy().getClass() == te.getClass()) reloaded++;
                else failed.add(BuiltInRegistries.BLOCK.getKey(b) + " (" + te.getClass().getSimpleName() + " not reloaded from " + tag.getCompound("legacy").getString("id") + ")");
            } catch (RuntimeException | LinkageError e) {
                failed.add(BuiltInRegistries.BLOCK.getKey(b) + " (" + e + ")");
            } finally {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        LOG.info("B1.7.3 TILE ENTITY TEST: {} blocks with a tile entity, {} made one, {} saved and loaded back", withEntity, made, reloaded);
        for (String f : failed.subList(0, Math.min(20, failed.size()))) LOG.info("  tile entity: {}", f);
        guis(level, blocks, base.above(4));
    }

    /**
     * Right-clicks every block (blocks with a tile entity with every metadata: IndustrialCraft 2's machines differ by it) with a server player
     * (a plain {@code ServerPlayer} on a {@link TestConnection}, which sends nothing: a fake player itself opens no menus), so
     * {@code OpenGUI} goes the whole way to {@code ServerPlayer.openMenu} and a {@link LegacyB173Menu}. In each menu the
     * client's slot layout is rebuilt from the open-screen extra data, a stack of coal is shift-clicked from the player's inventory into the
     * container and back, a crafting grid gets a log and its result is taken, the menu is updated ({@code updateCraftingResults}) and closed.
     * Also looks up a smelting and a crafting result the way mods' furnaces and tables do.
     */
    private static void guis(ServerLevel level, List<LegacyB173Block> blocks, BlockPos base) {
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("b173-gui-test".getBytes()), "[B173GuiTest]"),
                net.minecraft.server.level.ClientInformation.createDefault());
        new TestConnection(level.getServer(), player);
        ItemUseSelfTestB173.run(level, player, base);
        // single player: the client's and the server's player share an entity id (Entity.equals compares ids); each needs its own view
        var twin = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int twinId = twin.getId();
        twin.setId(player.getId());
        boolean ownViews = PlayerViewB173.of(twin) != PlayerViewB173.of(player) && PlayerViewB173.of(player).mcc$modern == player;
        twin.setId(twinId);
        int tried = 0, opened = 0, slots = 0, legacySlots = 0, shiftIn = 0, shiftOut = 0, grids = 0, crafted = 0, failures = 0, layouts = 0, byItem = 0;
        java.util.Set<Class<?>> kinds = new java.util.LinkedHashSet<>();
        List<String> notes = new ArrayList<>();
        int n = 0;
        try {
            for (LegacyB173Block b : blocks) {
                int metas = b instanceof LegacyB173EntityBlock ? 16 : 1;
                for (int meta = 0; meta < metas; meta++) {
                    BlockPos pos = base.offset((n % 32) * 2, 0, (n / 32) * 2);
                    n++;
                    tried++;
                    String name = BuiltInRegistries.BLOCK.getKey(b) + (meta > 0 ? "@" + meta : "");
                    try {
                        BlockState state = b.defaultBlockState();
                        if (meta > 0) state = state.setValue(LegacyB173Block.META, meta);
                        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                        player.getInventory().clearContent();
                        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                        // placed from its item as a player would (the stack's metadata must reach onBlockAdded and the tile entity)
                        if (b.asItem() instanceof LegacyB173BlockItem) {
                            var stack = new net.minecraft.world.item.ItemStack(b.asItem());
                            if (meta > 0) stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                                    net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(LegacyB173Block.META, meta));
                            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                            stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos.below()).add(0, 0.5, 0),
                                            net.minecraft.core.Direction.UP, pos.below(), false)));
                            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                        }
                        if (level.getBlockState(pos).getBlock() == b) byItem++;
                        else level.setBlock(pos, state, 3);
                        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
                        level.getBlockState(pos).useWithoutItem(level, player, hit);
                        if (!(player.containerMenu instanceof LegacyB173Menu menu)) continue;
                        var container = menu.legacy();
                        opened++;
                        slots += menu.slots.size();
                        boolean first = kinds.add(container.getClass());
                        // the client's menu from the open-screen packet's extra data has the same slots
                        var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
                        LegacyB173Menu.write(buf, container, null, 0);
                        if (LegacyB173Menu.client(2, player.getInventory(), buf).slots.size() == menu.slots.size()) layouts++;
                        int playerSlot = -1, result = -1, grid = -1;
                        for (int i = 0; i < menu.slots.size(); i++) {
                            var s = menu.slots.get(i);
                            if (s instanceof LegacySlotB173 ls) {
                                legacySlots++;
                                if (ls.legacy() instanceof legacyconnector.vb1_7_3.net.minecraft.screen.slot.CraftingResultSlot) result = i;
                                else if (grid < 0 && ls.legacy().inventory instanceof legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory) grid = i;
                            } else if (s.container == player.getInventory() && s.getContainerSlot() == 9) {
                                playerSlot = i;
                            }
                        }
                        if (playerSlot >= 0) {
                            player.getInventory().setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 16));
                            menu.clicked(playerSlot, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                            int into = -1;
                            for (int i = 0; i < menu.slots.size(); i++) {
                                if (menu.slots.get(i) instanceof LegacySlotB173 ls && ls.getItem().is(net.minecraft.world.item.Items.COAL)) into = i;
                            }
                            if (into >= 0 && player.getInventory().getItem(9).getCount() < 16) {
                                shiftIn++;
                                menu.clicked(into, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
                                if (!menu.slots.get(into).getItem().is(net.minecraft.world.item.Items.COAL)) shiftOut++;
                            }
                        }
                        if (grid >= 0 && result >= 0) {
                            // one log in the grid: four planks in the result slot; taking them uses the log up
                            grids++;
                            menu.slots.get(grid).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
                            menu.broadcastChanges();
                            var out = menu.slots.get(result).getItem().copy();
                            if (!out.isEmpty()) {
                                menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                                menu.clicked(result, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                                if (!menu.getCarried().isEmpty() && menu.slots.get(grid).getItem().isEmpty()) crafted++;
                                else notes.add(name + ": crafting result " + out + " not taken (carried " + menu.getCarried() + ")");
                                menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                            } else {
                                notes.add(name + ": a log in the crafting grid makes nothing");
                            }
                        }
                        for (int t = 0; t < 5; t++) menu.broadcastChanges();
                        if (menu.failed()) {
                            failures++;
                            notes.add(name + " failed while updating");
                        }
                        player.closeContainer();
                        if (first) notes.add(name + " (" + container.getClass().getSimpleName() + "): " + menu.slots.size() + " slots");
                    } catch (RuntimeException | LinkageError e) {
                        failures++;
                        notes.add(name + " (" + e + ")");
                    } finally {
                        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        } finally {
            player.discard();
        }
        LOG.info("B1.7.3 GUI TEST: {} blocks/metadata right-clicked ({} placed from their item), {} opened a menu ({} container classes, {} slots, {} "
                + "of the containers; {} client layouts match); shift-click: {} took coal in, {} gave it back; crafting grids: {} of {} crafted; {} failed; "
                + "players with the same entity id get their own view: {}",
                tried, byItem, opened, kinds.size(), slots, legacySlots, layouts, shiftIn, shiftOut, crafted, grids, failures, ownViews);
        for (String s : notes.subList(0, Math.min(40, notes.size()))) LOG.info("  gui: {}", s);
        var smelted = legacyconnector.vb1_7_3.net.minecraft.recipe.SmeltingRecipeManager.getInstance().craft(15);
        var grid = new legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory(null, 2, 2);
        for (int i = 0; i < 4; i++) grid.setStack(i, new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(5, 1, 0));
        var table = legacyconnector.vb1_7_3.net.minecraft.recipe.CraftingRecipeManager.getInstance().craft(grid);
        LOG.info("B1.7.3 RECIPE LOOKUP: smelting iron ore (15) gives {}, four planks (5) craft {}", smelted, table);
    }

    private static void run(ServerLevel level) {
        List<LegacyB173Block> blocks = new ArrayList<>();
        for (var b : BuiltInRegistries.BLOCK) if (b instanceof LegacyB173Block l) blocks.add(l);
        int items = 0;
        for (var l : ContentB173.items()) if (l.mcc$owner != null && l.mcc$modern != null) items++;
        BlockPos base = level.getSharedSpawnPos().atY(Math.min(level.getMaxBuildHeight() - 4, 300));
        int placed = 0, drops = 0;
        List<String> failed = new ArrayList<>();
        int n = 0;
        for (LegacyB173Block b : blocks) {
            BlockPos pos = base.offset((n % 32) * 2, 0, (n / 32) * 2);
            n++;
            try {
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
                BlockState state = b.defaultBlockState();
                level.setBlock(pos, state, 2);
                if (level.getBlockState(pos).getBlock() == b) placed++;
                else failed.add(BuiltInRegistries.BLOCK.getKey(b) + " (not placed)");
                if (!net.minecraft.world.level.block.Block.getDrops(state, level, pos, null).isEmpty()) drops++;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 2);
            } catch (RuntimeException | LinkageError e) {
                failed.add(BuiltInRegistries.BLOCK.getKey(b) + " (" + e + ")");
            }
        }
        behaviour(level, blocks, base.above(4));
        int[] recipes = RecipesB173.counts();
        LOG.info("B1.7.3 SELF TEST: {} of {} blocks placed, {} drop something; {} items; recipes {} translated, {} not; {} texture overrides; "
                        + "{} tile entities and {} entities registered (not bridged yet)",
                placed, blocks.size(), drops, items, recipes[0], recipes[1], ContentB173.textureOverrides().size(), ContentB173.tileEntities().size(),
                ContentB173.entities().size());
        for (String f : failed.subList(0, Math.min(20, failed.size()))) LOG.info("  failed: {}", f);
        subtypes();
    }

    /** The GUI test player's connection: sends nothing and has no modded channels (menus send their data as vanilla packets). */
    private static final class TestConnection extends net.minecraft.server.network.ServerGamePacketListenerImpl {
        TestConnection(net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
            super(server, new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND), player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet) {}

        @Override
        public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}

        @Override
        public boolean hasChannel(net.minecraft.resources.ResourceLocation id) {
            return false;
        }

        @Override
        public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
            return false;
        }

        @Override
        public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
            return false;
        }
    }

    /** Block items with subtypes: how many have names per metadata, with the names of each. */
    private static void subtypes() {
        int with = 0, named = 0;
        List<String> lines = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof LegacyB173BlockItem bi) || !bi.hasSubtypes()) continue;
            with++;
            java.util.Set<String> names = new java.util.LinkedHashSet<>();
            for (int meta = 0; meta < 16; meta++) {
                var stack = new net.minecraft.world.item.ItemStack(bi);
                stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                        net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(LegacyB173Block.META, meta));
                String key = bi.getDescriptionId(stack);
                String name = org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations.get("en_us", key);
                names.add(name == null ? key : name);
            }
            if (names.size() > 1) named++;
            lines.add(BuiltInRegistries.ITEM.getKey(bi) + ": " + String.join(", ", names));
        }
        LOG.info("B1.7.3 SUBTYPES: {} block items with subtypes, {} named per metadata", with, named);
        for (String l : lines.subList(0, Math.min(10, lines.size()))) LOG.info("  subtypes: {}", l);
    }
}
