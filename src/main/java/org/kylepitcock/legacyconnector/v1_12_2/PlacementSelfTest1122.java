package org.kylepitcock.legacyconnector.v1_12_2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122BlockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development check ({@code -Dlegacyconnector.entitySelfTest=true}): a fake player places every item of the 1.12.2 mods that is an ItemBlock on a
 * stone block with {@code useOn} (the way a right click does) and the result is logged as "1.12.2 item placement: N of M placed, failing: ...".
 */
public final class PlacementSelfTest1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/PlacementTest");
    private static int ticks = -1;

    private PlacementSelfTest1122() {}

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ticks = 0);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (ticks < 0 || ticks > 30) return;
            if (++ticks == 30) {
                try {
                    run(e.getServer().overworld());
                } catch (RuntimeException | LinkageError ex) {
                    LOG.warn("1.12.2 item placement test failed", ex);
                }
            }
        });
    }

    /** Which step of BlockItem.place refused: the placement context, the placement state, or survival of the state at the position. */
    private static String why(net.minecraft.world.item.Item item, ServerLevel level, net.minecraft.world.entity.player.Player player, ItemStack stack,
            BlockHitResult hit) {
        try {
            var ctx = new net.minecraft.world.item.context.BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
            if (!ctx.canPlace()) return ": the position is not replaceable";
            var m = net.minecraft.world.item.BlockItem.class.getDeclaredMethod("getPlacementState", net.minecraft.world.item.context.BlockPlaceContext.class);
            m.setAccessible(true);
            var state = (net.minecraft.world.level.block.state.BlockState) m.invoke(item, ctx);
            if (state == null) {
                var b = ((net.minecraft.world.item.BlockItem) item).getBlock();
                var s = b.getStateForPlacement(ctx);
                if (s == null) return ": the block gave no placement state";
                if (!s.canSurvive(level, ctx.getClickedPos())) return ": the state cannot survive (canBlockStay/canPlaceBlockAt)";
                return ": getPlacementState was null (canPlace: collision or the player's own box)";
            }
            return ": placed state " + state + " but the item refused (its own onItemUse/placeBlockAt)"
                    + (System.getProperty("legacyconnector.placementDiag") != null ? legacyChecks(item, level, player, stack, ctx.getClickedPos()) : "");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return ": diagnosis threw " + e;
        }
    }

    /** The checks a legacy ItemBlock.onItemUse makes, one by one. */
    private static String legacyChecks(net.minecraft.world.item.Item item, ServerLevel level, net.minecraft.world.entity.player.Player player, ItemStack stack,
            BlockPos pos) {
        try {
            var legacy = ((Legacy1122BlockItem) item).legacy();
            var block = ((legacyconnector.v1_12_2.net.minecraft.item.ItemBlock) legacy).getBlock();
            var world = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level);
            var lpos = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos);
            var side = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP;
            var lstack = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(stack);
            var lplayer = (legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer) org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(player);
            StringBuilder sb = new StringBuilder(" [");
            sb.append("count=").append(lstack.getCount()).append(" meta=").append(lstack.getMetadata());
            sb.append(" canEdit=").append(lplayer.canPlayerEdit(lpos, side, lstack));
            sb.append(" replaceable=").append(block.isReplaceable(world, lpos));
            sb.append(" canPlaceOnSide=").append(block.canPlaceBlockOnSide(world, lpos, side));
            var bb = block.getBoundingBox(block.getStateFromMeta(legacy.getMetadata(lstack.getMetadata())), world, lpos);
            sb.append(" bb=").append(bb == null ? "null" : bb.offset(lpos));
            if (bb != null) sb.append(" noEntityCollision=").append(world.checkNoEntityCollision(bb.offset(lpos), null));
            var held = lplayer.getHeldItem(legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND);
            sb.append(" heldCount=").append(held.getCount()).append(" heldItem=").append(held.getItem() == legacy);
            sb.append(" canEditAbove=").append(lplayer.canPlayerEdit(lpos, side, held));
            sb.append(" onItemUse=").append(legacy.onItemUse(lplayer, world, lpos.down(), legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND, side, 0.5F, 1.0F, 0.5F));
            return sb.append(']').toString();
        } catch (RuntimeException | LinkageError e) {
            return " [legacy checks threw " + e + "]";
        }
    }

    private static void run(ServerLevel level) {
        var player = FakePlayerFactory.getMinecraft(level);
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos ground = new BlockPos(spawn.getX(), level.getMaxBuildHeight() - 40, spawn.getZ());
        int placed = 0, total = 0;
        List<String> failing = new ArrayList<>();
        List<String> sneaking = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof Legacy1122BlockItem)) continue;
            var id = BuiltInRegistries.ITEM.getKey(item);
            total++;
            String result;
            try {
                for (int dx = -2; dx <= 2; dx++) for (int dy = 0; dy <= 4; dy++) for (int dz = -2; dz <= 2; dz++) {
                    level.setBlock(ground.offset(dx, dy, dz), dy == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
                player.setPos(ground.getX() - 1.0, ground.getY() + 1, ground.getZ() + 0.5);
                ItemStack stack = new ItemStack(item);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                var hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
                InteractionResult r = item.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
                boolean there = !level.getBlockState(ground.above()).isAir();
                if (!there) {
                    // some items place only while sneaking (RS' portable grid opens its GUI otherwise)
                    level.removeBlockEntity(ground.above());
                    stack = new ItemStack(item);
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    player.setShiftKeyDown(true);
                    r = item.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
                    there = !level.getBlockState(ground.above()).isAir();
                    player.setShiftKeyDown(false);
                    if (there) sneaking.add(id.toString());
                }
                result = there ? null : "result " + r + why(item, level, player, stack, hit);
                if (there) placed++;
                level.removeBlockEntity(ground.above());
                level.setBlock(ground.above(), Blocks.AIR.defaultBlockState(), 2);
            } catch (RuntimeException | LinkageError ex) {
                result = "threw " + ex;
            }
            if (result != null) failing.add(id + " (" + result + ")");
        }
        LOG.info("1.12.2 item placement: {} of {} placed ({} only while sneaking: {}), failing: {}", placed, total, sneaking.size(), sneaking, failing);
    }
}
