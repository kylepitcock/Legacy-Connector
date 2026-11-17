package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The start of a mineshaft at a chunk. The modern game lays out mineshafts itself (the shim's {@link MapGenMineshaft} never asks for
 * a start), so this holds only the position for mods that name or subclass the class: its box is the 16 by 16 column of the chunk.
 */
public class StructureMineshaftStart extends StructureStart {
    public StructureMineshaftStart() {
    }

    public StructureMineshaftStart(World world, Random rand, int chunkX, int chunkZ) {
        super(chunkX, chunkZ);
        this.boundingBox = new StructureBoundingBox((chunkX << 4) + 2, 10, (chunkZ << 4) + 2, (chunkX << 4) + 2 + 7, 54, (chunkZ << 4) + 2 + 7);
    }
}
