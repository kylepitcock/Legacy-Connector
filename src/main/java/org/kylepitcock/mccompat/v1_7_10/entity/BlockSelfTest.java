package org.kylepitcock.mccompat.v1_7_10.entity;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mccompat.v1_7_10.net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.mccompat.v1_7_10.adapter.LegacyBlockEntity;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development check ({@code -Dretrofit.entitySelfTest=true}, with the entity test): places every legacy block (two
 * metadata values) in a grid high above spawn, lets the world tick, right-clicks each with a fake player, and
 * reports blocks whose placement, tile entity, ticking or activation threw. Everything is removed afterwards.
 */
public final class BlockSelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/BlockTest");
    private static final int[] METAS = {0, 1};
    private static final Map<String, String> PROBLEMS = new LinkedHashMap<>();
    private static final List<BlockPos> PLACED = new ArrayList<>();
    private static final List<String> NAMES = new ArrayList<>();
    private static int ticks = -1;
    private static int placed;
    private static int skipped;

    private BlockSelfTest() {}

    public static void register() {
        if (!Boolean.getBoolean("retrofit.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> start(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        // Other dev fixtures stop the server early (the fixture mod after its own test): report what there is.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> {
            if (ticks >= 0 && ticks < 60) {
                ticks = 59;
                tick(e.getServer());
            }
        });
    }

    private static void start(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int i = 0;
        // Inside the spawn chunks (already loaded): generating chunks one by one from here would take minutes.
        int baseX = spawn.getX() - 40, baseZ = spawn.getZ() - 40, y0 = level.getMaxBuildHeight() - 60;
        for (Block legacy : BlockBridge.modBlocks()) {
            if (legacy.mcc$modern == null) continue;
            var key = BuiltInRegistries.BLOCK.getKey(legacy.mcc$modern);
            if (key == null) continue;
            for (int meta : METAS) {
                int layer = i / 1600, cell = i % 1600;
                int x = baseX + (cell % 40) * 2, z = baseZ + (cell / 40) * 2, y = y0 + layer * 3;
                i++;
                BlockPos pos = new BlockPos(x, y, z);
                String name = key + (meta == 0 ? "" : "#" + meta);
                try {
                    if (!level.hasChunk(x >> 4, z >> 4)) {
                        skipped++;
                        continue;
                    }
                    BlockState state = legacy.mcc$modern.defaultBlockState();
                    if (legacy.mcc$modern instanceof LegacyBlockAdapter) {
                        state = state.setValue(LegacyBlockAdapter.META, meta);
                    }
                    level.setBlock(pos, state, 3);
                    PLACED.add(pos);
                    NAMES.add(name);
                    placed++;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof LegacyBlockEntity l && l.legacy() == null && l.createFailed()) PROBLEMS.putIfAbsent(name, "tile entity creation threw");
                } catch (RuntimeException | LinkageError ex) {
                    PROBLEMS.putIfAbsent(name, "placement threw " + describe(ex));
                }
            }
        }
        ticks = 0;
        LOG.info("Placed {} legacy blocks for the self test ({} outside the loaded area)", placed, skipped);
    }

    private static String describe(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        StringBuilder where = new StringBuilder();
        int frames = 0;
        for (StackTraceElement e : root.getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("net.minecraft.") || c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("com.mojang.")
                    || c.startsWith("net.neoforged.") || c.startsWith("mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer")) continue;
            where.append(frames == 0 ? " at " : " < ").append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName())
                    .append(':').append(e.getLineNumber());
            if (++frames == 3) break;
        }
        return root + where.toString();
    }

    private static void tick(MinecraftServer server) {
        if (ticks < 0 || ++ticks != 60) return;
        ServerLevel level = server.overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes("retrofit-block-test".getBytes()), "[RetrofitTest]"));
        int survived = 0, activated = 0;
        for (int i = 0; i < PLACED.size(); i++) {
            BlockPos pos = PLACED.get(i);
            String name = NAMES.get(i);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            survived++;
            try {
                player.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                state.useWithoutItem(level, player, hit);
                activated++;
                player.closeContainer();
            } catch (RuntimeException | LinkageError ex) {
                PROBLEMS.putIfAbsent(name, "activation threw " + describe(ex));
            }
        }
        for (BlockPos pos : PLACED) level.removeBlock(pos, false);
        Map<String, String> itemProblems = itemTest(level, player);
        StringBuilder sb = new StringBuilder();
        PROBLEMS.forEach((k, v) -> sb.append("\n  ").append(k).append(": ").append(v));
        LOG.info("BLOCK SELF TEST: {} placed, {} still there after 60 ticks, {} activated, {} with problems{}", placed, survived,
                activated, PROBLEMS.size(), sb);
        StringBuilder ib = new StringBuilder();
        itemProblems.forEach((k, v) -> ib.append("\n  ").append(k).append(": ").append(v));
        LOG.info("ITEM SELF TEST: {} items with problems{}", itemProblems.size(), ib);
    }

    /** Names, tooltips and right clicks (in the air and on the ground) of every legacy item. */
    private static Map<String, String> itemTest(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        Map<String, String> problems = new LinkedHashMap<>();
        BlockPos ground = level.getSharedSpawnPos().above(2);
        for (var item : org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge.modItems()) {
            for (var modern : item.mcc$variants.values()) {
                String name = String.valueOf(BuiltInRegistries.ITEM.getKey(modern));
                if (problems.containsKey(name)) continue;
                var stack = new net.minecraft.world.item.ItemStack(modern);
                String stage = "name";
                try {
                    modern.getName(stack);
                    stage = "tooltip";
                    stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(level), player, net.minecraft.world.item.TooltipFlag.ADVANCED);
                    stage = "use";
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
                    modern.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    stage = "useOn";
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
                    var hit = new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false);
                    modern.useOn(new net.minecraft.world.item.context.UseOnContext(level, player, net.minecraft.world.InteractionHand.MAIN_HAND,
                            player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND), hit));
                    player.closeContainer();
                } catch (RuntimeException | LinkageError ex) {
                    problems.put(name, stage + " threw " + describe(ex));
                }
            }
        }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        return problems;
    }
}
