package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;

/** A two-block-tall door (and the base of mods' doors). */
public class BlockDoor extends Block {
    public BlockDoor(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockDoor(Material material) {
        super(material);
    }

    /** Whether the door is open, from the metadata of its lower half. */
    public static boolean func_150015_f(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return (world.getBlockMetadata(x, y, z) & 4) != 0;
    }
}
