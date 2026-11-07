package legacyconnector.v1_6_4.facade.net.minecraft.world.chunk.storage;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code ExtendedBlockStorage} for 1.6.4 mods: blocks read and written as ids. Never instantiated. */
public final class ExtendedBlockStorage {
    private ExtendedBlockStorage() {}

    /** {@code getExtBlockID(x, y, z)}: the id of the block in this 16x16x16 section (0 for air). */
    public static int getExtBlockID(legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage self, int x, int y, int z) {
        return Ids.blockId(self.getBlockByExtId(x, y, z));
    }

    /** {@code setExtBlockID(x, y, z, id)}: puts the block of that id (air for 0 or an unused id). */
    public static void setExtBlockID(legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage self, int x, int y, int z, int id) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = Ids.block(id);
        self.func_150818_a(x, y, z, block == null ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : block);
    }
}
