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
        legacyBuildings(level, log, found);
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

    /**
     * The legacy mods' buildings of several villages (the one found above and a few more around it): which handler's building,
     * the blocks of mods inside its own bounding box, whether it stands against a street, and whether it overlaps another piece.
     * One summary line: {@code VILLAGE TEST SUMMARY: villages, legacy pieces, blocks of mods in them, overlapping}.
     */
    private static void legacyBuildings(ServerLevel level, org.slf4j.Logger log, BlockPos first) {
        var structures = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        BlockPos origin = level.getSharedSpawnPos();
        BlockPos at = first;
        int villages = 0, pieces = 0, modBlocks = 0, overlaps = 0, streetless = 0, vanillaPieces = 0;
        int[][] steps = {{2500, 0}, {0, 2500}, {-2500, 0}, {0, -2500}, {2500, 2500}, {-2500, -2500}, {2500, -2500}, {-2500, 2500}};
        // six villages, and on until every registered handler has built something (at most 14 villages)
        java.util.Set<String> built = new java.util.HashSet<>();
        for (int attempt = 0; attempt < 40 && villages < 14 && (villages < 6 || !missing(built).isEmpty()); attempt++) {
            if (attempt > 0) {
                int[] d = steps[(attempt - 1) % steps.length];
                int ring = 1 + (attempt - 1) / steps.length;
                at = level.findNearestMapStructure(StructureTags.VILLAGE, origin.offset(d[0] * ring, 0, d[1] * ring), 60, false);
                if (at == null) continue;
            }
            net.minecraft.world.level.ChunkPos here = new net.minecraft.world.level.ChunkPos(at);
            if (!seen.add(here.toLong())) continue;
            for (int x = here.x - 4; x <= here.x + 4; x++) for (int z = here.z - 4; z <= here.z + 4; z++) level.getChunk(x, z);
            net.minecraft.world.level.levelgen.structure.StructureStart start = null;
            for (int x = here.x - 8; x <= here.x + 8 && start == null; x++) {
                for (int z = here.z - 8; z <= here.z + 8 && start == null; z++) {
                    for (var s : level.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(x, z),
                            st -> structures.wrapAsHolder(st).is(StructureTags.VILLAGE))) {
                        if (s.isValid() && s.getBoundingBox().isInside(at.getX(), s.getBoundingBox().minY(), at.getZ())) {
                            start = s;
                            break;
                        }
                    }
                }
            }
            if (start == null) {
                log.info("VILLAGE TEST: no structure start found for the village near {} {}", at.getX(), at.getZ());
                continue;
            }
            villages++;
            var all = start.getPieces();
            java.util.List<net.minecraft.world.level.levelgen.structure.BoundingBox> streets = new java.util.ArrayList<>();
            for (var p : all) {
                if (p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pe && pe.getElement().toString().contains("street")) streets.add(p.getBoundingBox());
            }
            for (var p : all) {
                if (!(p instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pe)) continue;
                String element = pe.getElement().toString();
                if (!element.startsWith("LegacyVillage")) {
                    if (!element.contains("street")) vanillaPieces++;
                    continue;
                }
                built.add(element);
                var box = p.getBoundingBox();
                for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
                Map<String, Integer> ns = new TreeMap<>();
                Map<String, Integer> census = new TreeMap<>();
                int solid = 0;
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    var state = level.getBlockState(pos.set(x, y, z));
                    if (state.isAir()) continue;
                    solid++;
                    var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    census.merge(key.getPath(), 1, Integer::sum);
                    if (!key.getNamespace().equals("minecraft")) ns.merge(key.getNamespace(), 1, Integer::sum);
                }
                boolean beside = false;
                var grown = new net.minecraft.world.level.levelgen.structure.BoundingBox(box.minX() - 1, box.minY() - 8, box.minZ() - 1, box.maxX() + 1, box.maxY() + 8, box.maxZ() + 1);
                for (var s : streets) if (s.intersects(grown)) beside = true;
                int overlapping = 0;
                for (var o : all) if (o != p && o.getBoundingBox().intersects(box)) overlapping++;
                pieces++;
                for (int n : ns.values()) modBlocks += n;
                overlaps += overlapping;
                if (!beside) streetless++;
                if (solid < 120) log.info("VILLAGE TEST: sparse piece {}: blocks {}", element, census);
                log.info("VILLAGE TEST: village at {} {}: {} box {} {} {} to {} {} {}: {} solid blocks, mods {}, beside a street {}, overlapping pieces {}",
                        at.getX(), at.getZ(), element, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(), solid, ns, beside, overlapping);
            }
        }
        for (String m : missing(built)) log.info("VILLAGE TEST: handler {} built nothing in {} villages", m, villages);
        log.info("VILLAGE TEST SUMMARY: {} villages, {} legacy pieces, {} blocks of mods in them, {} overlapping, {} away from a street, {} vanilla buildings",
                villages, pieces, modBlocks, overlaps, streetless, vanillaPieces);
    }

    /** The registered village creation handlers none of whose buildings is among the elements seen so far. */
    private static java.util.List<String> missing(java.util.Set<String> built) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var h : org.kylepitcock.legacyconnector.v1_7_10.bridge.VillagerBridge.creationHandlers()) {
            String key = org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyVillagePoolElement.key(h);
            boolean seen = false;
            for (String e : built) if (e.contains("[" + key + " in ")) seen = true;
            if (!seen) out.add(key);
        }
        return out;
    }
}
