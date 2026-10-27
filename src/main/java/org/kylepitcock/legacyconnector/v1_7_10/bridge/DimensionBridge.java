package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.world.WorldProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Legacy numeric dimension ids: 0 overworld, -1 nether, 1 end, other dimensions by hashed id. */
public final class DimensionBridge {
    private DimensionBridge() {}

    public static ServerLevel level(MinecraftServer server, int id) {
        ServerLevel loaded = loaded(server, id);
        if (loaded != null) return loaded;
        // a mod dimension registered while the server runs: its world is created on first use, as in 1.7.10
        return org.kylepitcock.legacyconnector.v1_7_10.dimension.RuntimeDimensions.getOrCreate(server, id);
    }

    /** The level of a dimension that exists already, without creating one (packets sent to a dimension nobody is in). */
    public static ServerLevel loaded(MinecraftServer server, int id) {
        for (ServerLevel level : server.getAllLevels()) {
            if (WorldProvider.dimensionId(level.dimension()) == id) return level;
        }
        return null;
    }
}
