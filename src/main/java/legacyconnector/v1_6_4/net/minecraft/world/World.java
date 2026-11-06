package legacyconnector.v1_6_4.net.minecraft.world;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.block.Block;

/**
 * Facade of the 1.7.10 shim's {@code World} for 1.6.4 mods: the methods that took block ids. A block id of 0 (or an unused one)
 * means air. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class World {
    private World() {}

    private static Block blockOf(int id) {
        Block b = id <= 0 ? null : Ids.block(id);
        return b != null ? b : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
    }

    public static boolean setBlock(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id, int meta, int flags) {
        return self.setBlock(x, y, z, blockOf(id), meta, flags);
    }

    public static boolean setBlock(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        return self.setBlock(x, y, z, blockOf(id), 0, 3);
    }

    public static void notifyBlocksOfNeighborChange(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.notifyBlocksOfNeighborChange(x, y, z, blockOf(id));
    }

    public static void notifyBlockOfNeighborChange(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.notifyBlockOfNeighborChange(x, y, z, blockOf(id));
    }

    public static void notifyBlockChange(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id) {
        self.notifyBlockChange(x, y, z, blockOf(id));
    }

    public static void scheduleBlockUpdate(legacyconnector.v1_7_10.net.minecraft.world.World self, int x, int y, int z, int id, int delay) {
        self.scheduleBlockUpdate(x, y, z, blockOf(id), delay);
    }
}
