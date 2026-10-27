package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.level.Level;

/** Legacy views of modern levels (server levels are {@code WorldServer}s) and 1.12's numeric dimension ids. */
public final class WorldBridge1122 {
    private static final Map<Level, World> WORLDS = new WeakHashMap<>();
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Integer> IDS = new java.util.concurrent.ConcurrentHashMap<>();

    private WorldBridge1122() {}

    public static synchronized World wrap(Level level) {
        if (level == null) return null;
        return WORLDS.computeIfAbsent(level, l -> l instanceof net.minecraft.server.level.ServerLevel s ? new ServerView(s) : new WorldView(l));
    }

    /**
     * The 1.12 dimension id of a level: 0, -1 and 1 for the vanilla three, the id a mod registered for a dimension of that name,
     * else a stable id of the level's name above 1000 (mods compare against 0 for the overworld, so a new dimension must not be 0).
     */
    public static int dimensionId(Level level) {
        var key = level.dimension();
        Integer known = IDS.get(key);
        if (known != null) return known;
        int id;
        if (key == Level.OVERWORLD) id = 0;
        else if (key == Level.NETHER) id = -1;
        else if (key == Level.END) id = 1;
        else {
            id = 1000 + Math.floorMod(key.location().toString().hashCode(), 1_000_000);
            for (var type : legacyconnector.v1_12_2.net.minecraft.world.DimensionType.values()) {
                if (type.getName().equals(key.location().getPath())) id = type.getId();
            }
        }
        IDS.put(key, id);
        return id;
    }

    /** The server level of a 1.12 dimension id, or null. */
    public static net.minecraft.server.level.ServerLevel levelOf(net.minecraft.server.MinecraftServer server, int id) {
        if (server == null) return null;
        if (id == 0) return server.overworld();
        if (id == -1) return server.getLevel(Level.NETHER);
        if (id == 1) return server.getLevel(Level.END);
        for (var level : server.getAllLevels()) if (dimensionId(level) == id) return level;
        return null;
    }

    private static final class WorldView extends World {
        WorldView(Level level) {
            super(level);
        }
    }

    private static final class ServerView extends legacyconnector.v1_12_2.net.minecraft.world.WorldServer {
        ServerView(net.minecraft.server.level.ServerLevel level) {
            super(level);
        }
    }
}
