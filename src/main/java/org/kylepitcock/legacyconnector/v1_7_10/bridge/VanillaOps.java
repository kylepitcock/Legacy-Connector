package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Helpers for the shim's ports of 1.7.10 vanilla block, item and tile entity logic that act on the modern game: the live level
 * behind a legacy world, positions, random sources and the icons of vanilla textures.
 */
public final class VanillaOps {
    private VanillaOps() {}

    /** The server level behind a legacy world; null on the client and while the world is a generation region. */
    public static ServerLevel server(World world) {
        return world != null && world.mcc$liveLevel() instanceof ServerLevel s ? s : null;
    }

    /** The live level behind a legacy world (client or server); null while it is a generation region. */
    public static Level live(World world) {
        return world == null ? null : world.mcc$liveLevel();
    }

    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    /** The modern state at a legacy position, or null when the world has no live level. */
    public static BlockState state(World world, int x, int y, int z) {
        Level level = live(world);
        return level == null ? null : level.getBlockState(new BlockPos(x, y, z));
    }

    /** A modern random source seeded from a legacy one (null: the level's). */
    public static RandomSource random(java.util.Random random, Level level) {
        if (random == null) return level != null ? level.random : RandomSource.create();
        return RandomSource.create(random.nextLong());
    }

    /** The icon of a vanilla item texture under its 1.7.10 name ("empty_armor_slot_helmet"). */
    public static IIcon itemIcon(String name) {
        return name == null ? null : Icons.ITEMS.registerIcon("minecraft:" + name);
    }

    /** The icon of a vanilla block texture under its 1.7.10 name ("grass_side_overlay"). */
    public static IIcon blockIcon(String name) {
        return name == null ? null : Icons.BLOCKS.registerIcon("minecraft:" + name);
    }

    private static final class Icons {
        static final legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap ITEMS =
                new legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(1, "textures/items");
        static final legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap BLOCKS =
                new legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(0, "textures/blocks");
    }
}
