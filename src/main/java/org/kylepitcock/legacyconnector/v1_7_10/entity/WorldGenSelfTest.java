package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyBiomes;
import org.slf4j.LoggerFactory;

/**
 * Development check (with {@code -Dlegacyconnector.entitySelfTest=true}): generates the chunks around spawn and reports the
 * surface biomes, how many chunks legacy biome decoration ran in, and which mod blocks ended up near the surface.
 */
public final class WorldGenSelfTest {
    private static final int RADIUS = 6;

    private WorldGenSelfTest() {}

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> run(e.getServer()));
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int cx = spawn.getX() >> 4, cz = spawn.getZ() >> 4;
        Map<String, Integer> biomes = new TreeMap<>();
        Map<String, Integer> blocks = new TreeMap<>();
        Map<String, Integer> floating = new TreeMap<>();
        Map<String, Integer> rubber = new TreeMap<>();
        Map<String, Integer> byId = new java.util.HashMap<>();
        int before = LegacyBiomes.DECORATED.get();
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                var chunk = level.getChunk(x, z);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        int wx = (x << 4) + dx, wz = (z << 4) + dz;
                        int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz);
                        if (dx % 8 == 0 && dz % 8 == 0) {
                            String b = level.getBiome(pos.set(wx, top, wz)).unwrapKey().map(k -> k.location().toString()).orElse("?");
                            biomes.merge(b, 1, Integer::sum);
                        }
                        for (int y = top - 6; y <= top + 1; y++) {
                            var id = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos.set(wx, y, wz)).getBlock());
                            if (!id.getNamespace().equals("minecraft")) {
                                if (id.getPath().contains("rubber")) rubber.merge(id.toString(), 1, Integer::sum);
                                byId.merge(id.toString(), 1, Integer::sum);
                                blocks.merge(id.getNamespace(), 1, Integer::sum);
                                // a mod block with air below it and not a tree part: planted in mid air
                                var self = chunk.getBlockState(pos.set(wx, y, wz));
                                if (!self.is(net.minecraft.tags.BlockTags.LEAVES) && !self.is(net.minecraft.tags.BlockTags.LOGS)
                                        && chunk.getBlockState(pos.set(wx, y - 1, wz)).isAir()) {
                                    floating.merge(id.getNamespace() + ":" + id.getPath(), 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }
        int chunks = (2 * RADIUS + 1) * (2 * RADIUS + 1);
        LoggerFactory.getLogger("LegacyConnector/WorldGenTest").info("WORLDGEN SELF TEST: {} chunks, legacy decoration ran in {} chunk(s) so far ({} during this test); "
                + "mod blocks near the surface by mod: {}; surface biomes: {}; mod blocks with air below: {}", chunks, LegacyBiomes.DECORATED.get(),
                LegacyBiomes.DECORATED.get() - before, blocks, biomes, floating);
        StringBuilder top = new StringBuilder();
        byId.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(40).forEach(e -> top.append("\n  ").append(e.getKey()).append(" ").append(e.getValue()));
        LoggerFactory.getLogger("LegacyConnector/WorldGenTest").info("WORLDGEN most common mod blocks near the surface:{}", top);
        if (!rubber.isEmpty()) LoggerFactory.getLogger("LegacyConnector/WorldGenTest").info("WORLDGEN rubber blocks near the surface in {} chunks: {}", chunks, rubber);
    }
}
