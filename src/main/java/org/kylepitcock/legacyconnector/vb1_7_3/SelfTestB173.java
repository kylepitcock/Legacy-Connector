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
        int[] recipes = RecipesB173.counts();
        LOG.info("B1.7.3 SELF TEST: {} of {} blocks placed, {} drop something; {} items; recipes {} translated, {} not; {} texture overrides; "
                        + "{} tile entities and {} entities registered (not bridged yet)",
                placed, blocks.size(), drops, items, recipes[0], recipes[1], ContentB173.textureOverrides().size(), ContentB173.tileEntities().size(),
                ContentB173.entities().size());
        for (String f : failed.subList(0, Math.min(20, failed.size()))) LOG.info("  failed: {}", f);
    }
}
