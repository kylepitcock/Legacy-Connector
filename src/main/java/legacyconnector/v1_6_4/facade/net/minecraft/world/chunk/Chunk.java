package legacyconnector.v1_6_4.facade.net.minecraft.world.chunk;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code Chunk} for 1.6.4 mods: {@code getBlockID}. See {@code Hierarchy.facadeOf}. */
public final class Chunk {
    private Chunk() {}

    public static int getBlockID(legacyconnector.v1_7_10.net.minecraft.world.chunk.Chunk self, int x, int y, int z) {
        return Ids.blockId(self.getBlock(x, y, z));
    }
}
