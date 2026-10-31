package org.kylepitcock.legacyconnector.compat.create;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create (and everything built on its contraption assembly: bearings, pistons, gantries, minecart contraptions, trains, Create
 * Aeronautics' physics assembly) asks {@link BlockMovementChecks} whether a block may be moved. Its fallback already refuses blocks
 * with destroy speed -1 and push reaction BLOCK, which legacy blocks get from their static hardness and Material mobility. These
 * checks add the legacy rules the static block properties cannot express:
 * <ul>
 *     <li>1.7.10's BlockPistonBase.canPushBlock refused {@code getMobilityFlag() == 2} (a per-block override, not only the Material)
 *     and {@code getBlockHardness(world, x, y, z) == -1} (hardness may depend on the position: warded or owned blocks);</li>
 *     <li>1.12.2 refused {@code getBlockHardness(state, world, pos) == -1} the same way (its push reaction is already delegated);</li>
 *     <li>blocks the pistons destroyed (mobility 1 / EnumPushReaction.DESTROY: torches, levers, plants, wires) need a support block,
 *     which is what Create calls brittle: they are taken off first and put back last.</li>
 * </ul>
 * Pistons also refused every block with a tile entity; Create is the point of moving them, so that rule is not carried over (the
 * hosted tile entity's NBT travels with the block, see LegacyBlockEntity).
 * <p>Only loaded when Create is installed.
 */
public final class CreateCompat {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Create");
    private static final java.util.Set<String> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private CreateCompat() {}

    public static void init() {
        BlockMovementChecks.registerMovementAllowedCheck(CreateCompat::movementAllowed);
        BlockMovementChecks.registerBrittleCheck(CreateCompat::brittle);
        LOG.info("Create found: legacy blocks follow their piston rules on contraptions");
    }

    private static CheckResult movementAllowed(BlockState state, Level level, BlockPos pos) {
        try {
            if (state.getBlock() instanceof LegacyBlockAdapter adapter) {
                var block = (legacyconnector.v1_7_10.net.minecraft.block.Block) adapter.legacy();
                if (block.getMobilityFlag() == 2) return CheckResult.FAIL;
                if (block.getBlockHardness(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level), pos.getX(), pos.getY(), pos.getZ()) < 0) {
                    return CheckResult.FAIL;
                }
            } else if (state.getBlock() instanceof Legacy1122Block adapter) {
                var world = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
                if (adapter.legacy().getBlockHardness(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(state), world,
                        legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos)) < 0) {
                    return CheckResult.FAIL;
                }
            }
        } catch (RuntimeException | LinkageError e) {
            // A legacy block whose hardness needs more of the world than this view offers: leave the decision to Create.
            if (FAILED.add("allowed:" + state.getBlock().getClass().getName())) {
                LOG.warn("Movement check of {} failed; Create decides by itself: {}", state.getBlock(), e.toString());
            }
        }
        return CheckResult.PASS;
    }

    private static CheckResult brittle(BlockState state) {
        try {
            if (state.getBlock() instanceof LegacyBlockAdapter adapter) {
                if (((legacyconnector.v1_7_10.net.minecraft.block.Block) adapter.legacy()).getMobilityFlag() == 1) return CheckResult.SUCCESS;
            } else if (state.getBlock() instanceof Legacy1122Block adapter) {
                var reaction = adapter.legacy().getPushReaction(org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toLegacy(state));
                if (reaction == legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction.DESTROY) return CheckResult.SUCCESS;
            }
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add("brittle:" + state.getBlock().getClass().getName())) {
                LOG.warn("Brittle check of {} failed; Create decides by itself: {}", state.getBlock(), e.toString());
            }
        }
        return CheckResult.PASS;
    }
}
