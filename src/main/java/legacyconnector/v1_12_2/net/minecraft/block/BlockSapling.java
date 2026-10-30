package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 saplings: six woods (bits 0-2) and a growth stage (bit 3). In the light they grow a stage, then a tree: the modern tree of
 * the wood (big oaks, 2x2 spruce, jungle and dark oak trees included), grown by the modern sapling's tree grower.
 */
public class BlockSapling extends BlockBush implements IGrowable {
    public static final PropertyEnum<BlockPlanks.EnumType> TYPE = PropertyEnum.create("type", BlockPlanks.EnumType.class);
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);
    protected static final AxisAlignedBB SAPLING_AABB = new AxisAlignedBB(0.09999999403953552D, 0.0D, 0.09999999403953552D, 0.8999999761581421D,
            0.800000011920929D, 0.8999999761581421D);

    protected BlockSapling() {
        setDefaultState(blockState.getBaseState().withProperty(TYPE, BlockPlanks.EnumType.OAK).withProperty(STAGE, 0));
        setCreativeTab(CreativeTabs.DECORATIONS);
        setSoundType(SoundType.PLANT);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SAPLING_AABB;
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + "."
                + BlockPlanks.EnumType.OAK.getTranslationKey() + ".name");
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote) return;
        super.updateTick(worldIn, pos, state, rand);
        if (!worldIn.isAreaLoaded(pos, 1)) return;
        if (worldIn.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) grow(worldIn, pos, state, rand);
    }

    /** A stage, then the tree. */
    public void grow(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(STAGE) == 0) worldIn.setBlockState(pos, state.cycleProperty(STAGE), 4);
        else generateTree(worldIn, pos, state, rand);
    }

    /** Grows the tree of the sapling's wood with the modern tree grower (the sapling stays when there is no room). */
    public void generateTree(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!(worldIn.mcc$level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        String wood = state.getPropertyKeys().contains(TYPE) ? state.getValue(TYPE).getName() : "oak";
        var sapling = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(wood + "_sapling"));
        if (!(sapling instanceof net.minecraft.world.level.block.SaplingBlock modern)) return;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
        var original = level.getBlockState(mp);
        var placed = modern.defaultBlockState().setValue(net.minecraft.world.level.block.SaplingBlock.STAGE, 1);
        level.setBlock(mp, placed, 4);
        modern.advanceTree(level, mp, placed, level.getRandom());
        // no room for the tree: the legacy sapling comes back
        if (level.getBlockState(mp).getBlock() == modern) level.setBlock(mp, original, 4);
    }

    /** Whether the four saplings of a 2x2 tree are there with this one at the corner. */
    private boolean isTwoByTwoOfType(World worldIn, BlockPos pos, int x, int z, BlockPlanks.EnumType type) {
        return isTypeAt(worldIn, pos.add(x, 0, z), type) && isTypeAt(worldIn, pos.add(x + 1, 0, z), type)
                && isTypeAt(worldIn, pos.add(x, 0, z + 1), type) && isTypeAt(worldIn, pos.add(x + 1, 0, z + 1), type);
    }

    public boolean isTypeAt(World worldIn, BlockPos pos, BlockPlanks.EnumType type) {
        IBlockState state = worldIn.getBlockState(pos);
        return state.getBlock() == this && state.getValue(TYPE) == type;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(TYPE).getMetadata();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (BlockPlanks.EnumType t : BlockPlanks.EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return true;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return worldIn.rand.nextFloat() < 0.45D;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        grow(worldIn, pos, state, rand);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE, BlockPlanks.EnumType.byMetadata(meta & 7)).withProperty(STAGE, (meta & 8) >> 3);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE).getMetadata() | state.getValue(STAGE) << 3;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE, STAGE);
    }
}
