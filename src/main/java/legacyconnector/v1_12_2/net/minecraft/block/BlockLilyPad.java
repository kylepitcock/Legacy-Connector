package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 lily pad: floats on still water (or ice), breaks boats that hit it. */
public class BlockLilyPad extends BlockBush {
    protected static final AxisAlignedBB LILY_PAD_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.09375D, 0.9375D);

    protected BlockLilyPad() {
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return LILY_PAD_AABB;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return state.getMaterial() == Material.WATER || state.getMaterial() == Material.ICE;
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        if (pos.getY() < 0 || pos.getY() >= 256) return false;
        IBlockState below = worldIn.getBlockState(pos.down());
        Material m = below.getMaterial();
        return m == Material.WATER && below.getPropertyKeys().contains(BlockLiquid.LEVEL) && below.getValue(BlockLiquid.LEVEL) == 0 || m == Material.ICE;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }
}
