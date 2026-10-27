package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.level.Level;

public final class WorldBridge1122 {
    private static final Map<Level, World> WORLDS = new WeakHashMap<>();

    private WorldBridge1122() {}

    public static synchronized World wrap(Level level) {
        if (level == null) return null;
        // the client's level is a WorldClient (Minecraft.world)
        return WORLDS.computeIfAbsent(level, l -> l.isClientSide() ? new legacyconnector.v1_12_2.net.minecraft.client.multiplayer.WorldClient(l) : new WorldView(l));
    }

    private static final class WorldView extends World {
        WorldView(Level level) {
            super(level);
        }
    }
}
