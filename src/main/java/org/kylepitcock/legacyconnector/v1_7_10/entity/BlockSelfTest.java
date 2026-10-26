package org.kylepitcock.legacyconnector.v1_7_10.entity;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
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
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development check ({@code -Dlegacyconnector.entitySelfTest=true}, with the entity test): places every legacy block (two
 * metadata values) in a grid high above spawn, lets the world tick, right-clicks each with a fake player, and
 * reports blocks whose placement, tile entity, ticking or activation threw. Everything is removed afterwards.
 */
public final class BlockSelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/BlockTest");
    private static final int[] METAS = {0, 1};
    private static final Map<String, String> PROBLEMS = new LinkedHashMap<>();
    private static final List<BlockPos> PLACED = new ArrayList<>();
    private static final List<String> NAMES = new ArrayList<>();
    private static int ticks = -1;
    private static int placed;
    private static int skipped;
    private static boolean stopping;

    private BlockSelfTest() {}

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> start(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tick(e.getServer()));
        // Other dev fixtures stop the server early (the fixture mod after its own test): report what there is.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> {
            if (ticks >= 0 && ticks < 60) {
                stopping = true;
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
        startFluids(level, baseX, baseZ + 84, y0 + 20);
        biomeArrayTest(level, spawn);
        ticks = 0;
        LOG.info("Placed {} legacy blocks for the self test ({} outside the loaded area)", placed, skipped);
    }

    /**
     * What Biome Wand does: write a legacy biome id into {@code chunk.getBiomeArray()} and set {@code isModified}
     * (mod bytecode reaches the accessor). The modern chunk must report the new biome afterwards.
     */
    private static void biomeArrayTest(ServerLevel level, BlockPos spawn) {
        try {
            var world = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            var chunk = world.getChunkFromBlockCoords(spawn.getX(), spawn.getZ());
            int index = (spawn.getZ() & 15) << 4 | (spawn.getX() & 15);
            byte[] biomes = chunk.getBiomeArray();
            byte before = biomes[index];
            byte target = (byte) (before == 2 ? 6 : 2); // desert, or swampland when it already is desert
            biomes[index] = target;
            legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk.mcc$set$isModified(chunk, true);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ());
            // the biome cell itself (getBiome would blur the position across neighbouring cells)
            var now = level.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(spawn.getX()), net.minecraft.core.QuartPos.fromBlock(y),
                    net.minecraft.core.QuartPos.fromBlock(spawn.getZ())).unwrapKey().map(k -> k.location().toString()).orElse("?");
            String expected = target == 2 ? "minecraft:desert" : "minecraft:swamp";
            if (now.equals(expected)) LOG.info("Biome array write-back: ok ({} -> {})", before, now);
            else LOG.warn("Biome array write-back FAILED: wrote legacy id {} (expected {}), world now has {} (chunk view {})", target, expected, now,
                    chunk.mcc$modern() == null ? "detached" : chunk.mcc$modern().getClass().getSimpleName());
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("Biome array write-back test threw {}", describe(ex));
        }
    }

    private static final List<Object[]> FLUIDS = new ArrayList<>();

    /** A stone floor with a source block of each flowing legacy fluid on it: the fluid must spread by scheduled ticks. */
    private static void startFluids(ServerLevel level, int baseX, int z, int y) {
        int k = 0;
        for (Block legacy : BlockBridge.modBlocks()) {
            if (!(legacy instanceof legacyconnector.v1_7_10.net.minecraftforge.fluids.BlockFluidBase) || legacy.mcc$modern == null) continue;
            if (k >= 6) break;
            int x = baseX + 4 + k * 12;
            k++;
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            try {
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                    level.setBlock(new BlockPos(x + dx, y - 1, z + dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                    for (int dy = 0; dy < 6; dy++) {
                        level.setBlock(new BlockPos(x + dx, y + dy, z + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                BlockPos pos = new BlockPos(x, y + 3, z);
                level.setBlock(pos, legacy.mcc$modern.defaultBlockState(), 3);
                FLUIDS.add(new Object[] {BuiltInRegistries.BLOCK.getKey(legacy.mcc$modern).toString(), legacy.mcc$modern, x, y, z});
            } catch (RuntimeException | LinkageError ex) {
                PROBLEMS.putIfAbsent("fluid " + legacy, "placement threw " + describe(ex));
            }
        }
    }

    private static void checkFluids(ServerLevel level) {
        for (Object[] f : FLUIDS) {
            net.minecraft.world.level.block.Block block = (net.minecraft.world.level.block.Block) f[1];
            int x = (Integer) f[2], y = (Integer) f[3], z = (Integer) f[4], count = 0, floor = 0;
            // (no player is near, so the world's own scheduled ticks do not run: tick the fluid blocks directly)
            try {
                for (int round = 0; round < 40; round++) {
                    for (int dy = 5; dy >= 0; dy--) for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                        BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                        BlockState st = level.getBlockState(p);
                        if (st.is(block)) st.tick(level, p, level.random);
                    }
                }
            } catch (RuntimeException | LinkageError ex) {
                PROBLEMS.putIfAbsent("fluid " + f[0], "tick threw " + describe(ex));
            }
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                if (level.getBlockState(new BlockPos(x + dx, y, z + dz)).is(block)) floor++;
                for (int dy = 0; dy < 6; dy++) if (level.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).is(block)) count++;
            }
            if (floor < 4) PROBLEMS.putIfAbsent("fluid " + f[0], "did not flow: " + count + " blocks, " + floor + " on the floor after 60 ticks");
            else LOG.info("Fluid {} flowed over {} floor blocks ({} blocks)", f[0], floor, count);
        }
    }

    private static String describe(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        StringBuilder where = new StringBuilder();
        int frames = 0;
        for (StackTraceElement e : root.getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("net.minecraft.") || c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("com.mojang.")
                    || c.startsWith("net.neoforged.") || c.startsWith("legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer")) continue;
            where.append(frames == 0 ? " at " : " < ").append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName())
                    .append(':').append(e.getLineNumber());
            if (++frames == 3) break;
        }
        return root + where.toString();
    }

    private static void tick(MinecraftServer server) {
        if (ticks < 0 || ++ticks != 60) return;
        ServerLevel level = server.overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes("legacyconnector-block-test".getBytes()), "[LegacyConnectorTest]"));
        checkFluids(level);
        int survived = 0, activated = 0;
        Map<String, Integer> vanished = new java.util.TreeMap<>();
        java.util.List<String> goneNames = new java.util.ArrayList<>();
        for (int i = 0; i < PLACED.size(); i++) {
            BlockPos pos = PLACED.get(i);
            String name = NAMES.get(i);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                vanished.merge(name.contains(":") ? name.substring(0, name.indexOf(':')) : name, 1, Integer::sum);
                goneNames.add(name);
                continue;
            }
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
        // (not while the server shuts down: mods' services are already stopped, and the world is discarded anyway)
        if (!stopping) {
            for (BlockPos pos : PLACED) {
                try {
                    level.removeBlock(pos, false);
                } catch (RuntimeException | LinkageError e) {
                    PROBLEMS.putIfAbsent("removal at " + pos.toShortString(), "threw " + describe(e));
                }
            }
        }
        LOG.info("Blocks gone after 60 ticks, per mod: {}", vanished);
        LOG.info("Blocks gone after 60 ticks: {}", goneNames);
        Map<String, String> itemProblems = itemTest(level, player);
        StringBuilder sb = new StringBuilder();
        PROBLEMS.forEach((k, v) -> sb.append("\n  ").append(k).append(": ").append(v));
        LOG.info("BLOCK SELF TEST: {} placed, {} still there after 60 ticks, {} activated, {} with problems{}", placed, survived,
                activated, PROBLEMS.size(), sb);
        StringBuilder ib = new StringBuilder();
        itemProblems.forEach((k, v) -> ib.append("\n  ").append(k).append(": ").append(v));
        LOG.info("ITEM SELF TEST: {} items with problems{}", itemProblems.size(), ib);
        LOG.info("TAB SELF TEST:{}", org.kylepitcock.legacyconnector.v1_7_10.bridge.CreativeTabBridge.mcc$selfTest());
    }

    /** Names, tooltips and right clicks (in the air and on the ground) of every legacy item. */
    private static Map<String, String> itemTest(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        Map<String, String> problems = new LinkedHashMap<>();
        BlockPos ground = level.getSharedSpawnPos().above(2);
        for (var item : org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.modItems()) {
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
