package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Base of rails; {@code field_150053_a} marks powered rails. */
public class BlockRailBase extends Block {
    protected final boolean field_150053_a;

    public BlockRailBase(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        this.field_150053_a = false;
    }

    protected BlockRailBase(boolean powered) {
        super(Material.circuits);
        this.field_150053_a = powered;
    }

    public static boolean func_150051_a(Block block) {
        return block instanceof BlockRailBase;
    }

    public static boolean isRailBlock(Block block) {
        return block instanceof BlockRailBase;
    }

    public boolean isFlexibleRail(IBlockAccess world, int x, int y, int z) {
        return !field_150053_a;
    }

    public boolean canMakeSlopes(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    public int getBasicRailMetadata(IBlockAccess world, Object cart, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        return field_150053_a ? meta & 7 : meta;
    }

    public float getRailMaxSpeed(World world, Object cart, int x, int y, int z) {
        return 0.4F;
    }

    public static boolean isRailBlockAt(World world, int x, int y, int z) {
        return isRailBlock(world.getBlock(x, y, z));
    }
}
