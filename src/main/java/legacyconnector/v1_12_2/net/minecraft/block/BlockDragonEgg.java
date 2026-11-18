package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 dragon egg: falls like sand and jumps to a free spot nearby when it is clicked. */
public class BlockDragonEgg extends BlockFalling {
    protected static final AxisAlignedBB DRAGON_EGG_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);

    public BlockDragonEgg() {
        super(Material.DRAGON_EGG);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return DRAGON_EGG_AABB;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        teleport(worldIn, pos);
        return true;
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        teleport(worldIn, pos);
    }

    private void teleport(World worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() != this) return;
        java.util.Random rand = worldIn.rand;
        for (int i = 0; i < 1000; ++i) {
            BlockPos target = pos.add(rand.nextInt(16) - rand.nextInt(16), rand.nextInt(8) - rand.nextInt(8), rand.nextInt(16) - rand.nextInt(16));
            if (worldIn.getBlockState(target).getBlock().getMaterial(worldIn.getBlockState(target)) == Material.AIR) {
                if (!worldIn.isRemote) {
                    worldIn.setBlockState(target, state, 2);
                    worldIn.setBlockToAir(pos);
                }
                return;
            }
        }
    }

    @Override
    public int tickRate(World worldIn) {
        return 5;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }
}
