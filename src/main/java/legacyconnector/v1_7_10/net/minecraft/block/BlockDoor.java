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

    /** Opens or closes the door (the lower half's open bit). */
    public void func_150014_a(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, boolean open) {
        int meta = world.getBlockMetadata(x, y, z);
        if (((meta & 4) != 0) == open) return;
        int lowerY = (meta & 8) != 0 ? y - 1 : y;
        int lower = world.getBlockMetadata(x, lowerY, z);
        world.setBlockMetadataWithNotify(x, lowerY, z, open ? lower | 4 : lower & ~4, 2);
    }

    /** Whether the door is open, from the metadata of its lower half. */
    public static boolean func_150015_f(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return (world.getBlockMetadata(x, y, z) & 4) != 0;
    }

    /** The lower half's facing and open bits, plus bit 8 for the upper half and bit 16 for a right-hinged door (as 1.7.10 combined both halves). */
    public static int getFullMetadata(legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        boolean upper = (meta & 8) != 0;
        int lower = upper ? world.getBlockMetadata(x, y - 1, z) : meta;
        int top = upper ? meta : world.getBlockMetadata(x, y + 1, z);
        return (lower & 7) | (upper ? 8 : 0) | ((top & 1) != 0 ? 16 : 0);
    }
}
