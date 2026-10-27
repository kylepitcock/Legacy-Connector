package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import net.minecraft.world.level.Level;

/** Client-only parts of the 1.12.2 world view; only called for client levels (the dedicated server has none of these classes). */
public final class WorldClient1122 {
    private WorldClient1122() {}

    public static Iterable<net.minecraft.world.entity.Entity> entities(Level level) {
        return level instanceof net.minecraft.client.multiplayer.ClientLevel c ? c.entitiesForRendering() : java.util.List.of();
    }

    public static net.minecraft.world.phys.Vec3 skyColor(Level level, net.minecraft.world.phys.Vec3 at, float partialTicks) {
        return level instanceof net.minecraft.client.multiplayer.ClientLevel c ? c.getSkyColor(at, partialTicks) : new net.minecraft.world.phys.Vec3(0.47D, 0.65D, 1.0D);
    }

    public static net.minecraft.world.phys.Vec3 cloudColor(Level level, float partialTicks) {
        return level instanceof net.minecraft.client.multiplayer.ClientLevel c ? c.getCloudColor(partialTicks) : new net.minecraft.world.phys.Vec3(1.0D, 1.0D, 1.0D);
    }

    /** Rebuilds the render sections of a block range. */
    public static void markRenderRange(int x1, int y1, int z1, int x2, int y2, int z2) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null) return;
        mc.levelRenderer.setBlocksDirty(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }
}
