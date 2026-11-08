package legacyconnector.v1_6_4.facade.net.minecraft.world.chunk;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code Chunk} for 1.6.4 mods: {@code getBlockID}. See {@code Hierarchy.facadeOf}. */
public final class Chunk {
    private Chunk() {}

    /** {@code setBlockIDWithMetadata(x, y, z, id, meta)}: places the block with that id (air for an unused id); true when the chunk changed. */
    public static boolean setBlockIDWithMetadata(legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk self, int x, int y, int z, int id, int meta) {
        legacyconnector.v1_7_10.net.minecraft.block.Block block = id == 0 ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : Ids.block(id);
        if (block == null) block = legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        return self.setBlockIDWithMetadata(x, y, z, block, meta);
    }

    public static int getBlockID(legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk self, int x, int y, int z) {
        return Ids.blockId(self.getBlock(x, y, z));
    }
}
