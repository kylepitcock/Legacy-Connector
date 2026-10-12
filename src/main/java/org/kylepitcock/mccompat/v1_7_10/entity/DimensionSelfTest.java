package org.kylepitcock.mccompat.v1_7_10.entity;

import java.util.Map;
import java.util.TreeMap;
import mccompat.v1_7_10.net.minecraftforge.common.DimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.kylepitcock.mccompat.v1_7_10.dimension.LegacyDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development check ({@code -Dretrofit.entitySelfTest=true}): generates a 5x5 chunk area of every mod dimension and
 * reports its blocks by namespace, so a dimension that generates nothing (or fails) is visible.
 */
public final class DimensionSelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/DimensionTest");

    private DimensionSelfTest() {}

    public static void register() {
        if (!Boolean.getBoolean("retrofit.entitySelfTest")) return;
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> run(e.getServer()));
    }

    private static void run(MinecraftServer server) {
        var dims = DimensionManager.mcc$modDimensions();
        if (dims.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        int ok = 0;
        for (int id : dims.keySet()) {
            ServerLevel level = server.getLevel(LegacyDimensions.levelKey(id));
            if (level == null) {
                sb.append("\n  dimension ").append(id).append(": no level");
                continue;
            }
            Map<String, Integer> byNamespace = new TreeMap<>();
            int solid = 0;
            long start = System.nanoTime();
            try {
                for (int cx = -2; cx <= 2; cx++) {
                    for (int cz = -2; cz <= 2; cz++) {
                        var chunk = level.getChunk(cx, cz);
                        for (int x = 0; x < 16; x += 2) {
                            for (int z = 0; z < 16; z += 2) {
                                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                                for (int y = Math.max(0, top - 3); y < Math.min(255, top + 2); y++) {
                                    BlockState state = chunk.getBlockState(new BlockPos(x, y, z));
                                    if (state.isAir()) continue;
                                    solid++;
                                    byNamespace.merge(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace(), 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
                ok++;
                sb.append("\n  dimension ").append(id).append(" (").append(dims.get(id).getSimpleName()).append("): ").append(solid)
                        .append(" blocks near the surface ").append(byNamespace).append(" in ").append((System.nanoTime() - start) / 1_000_000)
                        .append(" ms");
            } catch (RuntimeException | LinkageError ex) {
                sb.append("\n  dimension ").append(id).append(": generation threw ").append(ex);
            }
        }
        // An entity sent there through the legacy API (what portals call for mobs and items).
        try {
            for (int id : dims.keySet()) {
                var target = org.kylepitcock.mccompat.v1_7_10.bridge.DimensionBridge.level(server, id);
                if (target == null) continue;
                var item = new net.minecraft.world.entity.item.ItemEntity(server.overworld(), 0, 80, 0,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
                server.overworld().addFreshEntity(item);
                var view = org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.wrap(item);
                org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge.travelToDimension(view, id);
                sb.append("\n  entity transfer to ").append(id).append(": ").append(item.isRemoved() ? "moved" : "stayed");
            }
        } catch (RuntimeException | LinkageError ex) {
            sb.append("\n  transfer threw ").append(ex);
        }
        LOG.info("DIMENSION SELF TEST: {} of {} mod dimensions generated{}", ok, dims.size(), sb);
    }
}
