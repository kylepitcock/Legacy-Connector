package legacyconnector.v1_7_10.net.minecraft.world.gen;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The ravine carver. Mods with their own terrain create one next to the cave carver; the layer's modern world already has
 * vanilla ravines in the overworld, so this carves nothing and exists for subclasses and for generators that call it.
 */
public class MapGenRavine extends MapGenBase {
    @Override
    protected void func_151538_a(World world, int x, int z, int chunkX, int chunkZ, Block[] blocks) {
    }
}
