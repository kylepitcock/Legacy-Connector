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
    }
}
