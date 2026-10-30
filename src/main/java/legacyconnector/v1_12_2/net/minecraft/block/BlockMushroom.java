package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 small mushrooms: spread in the dark, stay on dim blocks (or mycelium and podzol at any light), and bone meal grows a huge
 * mushroom (the modern huge mushroom feature of the mushroom's kind).
 */
public class BlockMushroom extends BlockBush implements IGrowable {
    protected static final AxisAlignedBB MUSHROOM_AABB = new AxisAlignedBB(0.30000001192092896D, 0.0D, 0.30000001192092896D, 0.699999988079071D,
            0.4000000059604645D, 0.699999988079071D);

    protected BlockMushroom() {
        setTickRandomly(true);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return MUSHROOM_AABB;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (rand.nextInt(25) != 0) return;
        int i = 5;
        for (BlockPos p : BlockPos.getAllInBoxMutable(pos.add(-4, -1, -4), pos.add(4, 1, 4))) {
            if (worldIn.getBlockState(p).getBlock() == this && --i <= 0) return;
        }
        BlockPos target = pos.add(rand.nextInt(3) - 1, rand.nextInt(2) - rand.nextInt(2), rand.nextInt(3) - 1);
        for (int k = 0; k < 4; ++k) {
            if (worldIn.isAirBlock(target) && canBlockStay(worldIn, target, getDefaultState())) pos = target;
            target = pos.add(rand.nextInt(3) - 1, rand.nextInt(2) - rand.nextInt(2), rand.nextInt(3) - 1);
        }
        if (worldIn.isAirBlock(target) && canBlockStay(worldIn, target, getDefaultState())) worldIn.setBlockState(target, getDefaultState(), 2);
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos) && canBlockStay(worldIn, pos, getDefaultState());
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return state.isFullBlock();
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        if (pos.getY() < 0 || pos.getY() >= 256) return false;
        IBlockState below = worldIn.getBlockState(pos.down());
        if (Block.mcc$named(below.getBlock(), "mycelium")) return true;
        if (below.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.DIRT && below.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.PODZOL) return true;
        return worldIn.getLight(pos) < 13 && below.getBlock().canSustainPlant(below, worldIn, pos.down(),
                legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.UP, this);
    }

    /** Bone meal: the huge mushroom of this mushroom's kind (the small one stays when it does not fit). */
    public boolean generateBigMushroom(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!(worldIn.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        boolean red = Block.mcc$named(this, "red_mushroom");
        var modern = red ? net.minecraft.world.level.block.Blocks.RED_MUSHROOM : net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
        var original = level.getBlockState(mp);
        if (!(modern instanceof net.minecraft.world.level.block.MushroomBlock mushroom)) return false;
        level.setBlock(mp, modern.defaultBlockState(), 4);
        boolean grown = mushroom.growMushroom(level, mp, modern.defaultBlockState(), level.getRandom());
        if (!grown) level.setBlock(mp, original, 4);
        return grown;
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return rand.nextFloat() < 0.4D;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        generateBigMushroom(worldIn, pos, state, rand);
    }
}
