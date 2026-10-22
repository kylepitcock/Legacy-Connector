package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.LoggerFactory;

/**
 * Development check (with {@code -Dlegacyconnector.entitySelfTest=true}): finds the nearest village, generates its chunks and
 * reports the blocks of legacy mods inside its bounding box (legacy village components).
 */
public final class VillageSelfTest {
    private VillageSelfTest() {}

    public static void register() {
        if (!Boolean.getBoolean("legacyconnector.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> run(e.getServer()));
    }

    private static void run(MinecraftServer server) {
        var log = LoggerFactory.getLogger("LegacyConnector/VillageTest");
        ServerLevel level = server.overworld();
        BlockPos found = level.findNearestMapStructure(StructureTags.VILLAGE, level.getSharedSpawnPos(), 60, false);
        if (found == null) {
            log.info("VILLAGE TEST: no village within 60 chunks of spawn");
            return;
        }
        int cx = found.getX() >> 4, cz = found.getZ() >> 4;
        for (int x = cx - 5; x <= cx + 5; x++) for (int z = cz - 5; z <= cz + 5; z++) level.getChunk(x, z);
        Map<String, Integer> blocks = new TreeMap<>();
        int villagers = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = (cx - 4) << 4; x < (cx + 5) << 4; x++) {
            for (int z = (cz - 4) << 4; z < (cz + 5) << 4; z++) {
                for (int y = level.getMinBuildHeight(); y < 140; y++) {
                    var key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.set(x, y, z)).getBlock());
                    if (!key.getNamespace().equals("minecraft")) blocks.merge(key.getNamespace(), 1, Integer::sum);
                    if (key.getPath().contains("wall")) blocks.merge(key.toString(), 1, Integer::sum);
                    if (key.getPath().equals("wallgen")) {
                        var be = level.getBlockEntity(pos);
                        Object legacyTe = be instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity l ? l.legacy() : null;
                        String info = "no legacy tile entity";
                        if (legacyTe != null) {
                            try {
                                var f = legacyTe.getClass().getDeclaredField("bb");
                                f.setAccessible(true);
                                var ticksField = legacyTe.getClass().getSuperclass().getDeclaredField("ticks");
                                ticksField.setAccessible(true);
                                info = "bb " + f.get(legacyTe) + " ticks " + ticksField.get(legacyTe) + " canUpdate " + ((legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity) legacyTe).canUpdate();
                            } catch (ReflectiveOperationException ex) {
                                info = ex.toString();
                            }
                        }
                        log.info("VILLAGE TEST: wall generator at {} {} {}: {}", x, y, z, info);
                    }
                }
            }
        }
        for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.npc.Villager) villagers++;
        log.info("VILLAGE TEST: village near {} {}; blocks of mods in the 9x9 chunks around it: {}; villagers loaded: {}", found.getX(), found.getZ(), blocks, villagers);
        final int[] ticks = {0};
        final int fcx = cx, fcz = cz;
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            if (++ticks[0] != 600) return;
            int stone = 0, wallgen = 0;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = (fcx - 5) << 4; x < (fcx + 6) << 4; x++) for (int z = (fcz - 5) << 4; z < (fcz + 6) << 4; z++) for (int y = 60; y < 110; y++) {
                var key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.set(x, y, z)).getBlock());
                if (key.getPath().equals("wallgen")) wallgen++;
                if (key.getPath().endsWith("_wall") || key.getPath().equals("stone_bricks") || key.getPath().equals("cobblestone")) stone++;
            }
            log.info("VILLAGE TEST after 600 ticks: wall generators left {}, cobblestone/stone brick/wall blocks {}", wallgen, stone);
        });
    }
}
