package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityDaylightDetector;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 daylight sensor: a redstone source whose power follows the sky light (inverted: the darkness); a click swaps the normal and the
 * inverted block. Its tile entity re-reads the light once a second.
 */
public class BlockDaylightDetector extends BlockContainer {
    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);
    protected static final AxisAlignedBB DAYLIGHT_DETECTOR_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.375D, 1.0D);
    private final boolean inverted;

    public BlockDaylightDetector(boolean inverted) {
        super(Material.WOOD, MapColor.WOOD);
        this.inverted = inverted;
        setDefaultState(blockState.getBaseState().withProperty(POWER, 0));
        setHardness(0.2F);
        if (!inverted) setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return DAYLIGHT_DETECTOR_AABB;
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockState.getValue(POWER);
    }

    /** Sets the power from the sky light at the sensor (vanilla's formula: the sun's angle dims it towards dawn and dusk). */
    public void updatePower(World worldIn, BlockPos pos) {
        var level = worldIn.mcc$level();
        if (level == null || !level.dimensionType().hasSkyLight()) return;
        var mp = pos.mcc$modern();
        IBlockState state = worldIn.getBlockState(pos);
        int i = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, mp) - level.getSkyDarken();
        float f = level.getSunAngle(1.0F);
        if (inverted) i = 15 - i;
        if (i > 0 && !inverted) {
            float f1 = f < (float) Math.PI ? 0.0F : (float) Math.PI * 2F;
            f = f + (f1 - f) * 0.2F;
            i = Math.round(i * (float) Math.cos(f));
        }
        i = Math.max(0, Math.min(15, i));
        if (state.getPropertyKeys().contains(POWER) && state.getValue(POWER) != i) worldIn.setBlockState(pos, state.withProperty(POWER, i), 3);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (playerIn.isAllowEdit()) {
            if (worldIn.isRemote) return true;
            Block other = inverted ? legacyconnector.v1_12_2.net.minecraft.init.Blocks.DAYLIGHT_DETECTOR
                    : legacyconnector.v1_12_2.net.minecraft.init.Blocks.DAYLIGHT_DETECTOR_INVERTED;
            worldIn.setBlockState(pos, other.getDefaultState().withProperty(POWER, state.getValue(POWER)), 4);
            if (other instanceof BlockDaylightDetector d) d.updatePower(worldIn, pos);
            return true;
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(legacyconnector.v1_12_2.net.minecraft.init.Blocks.DAYLIGHT_DETECTOR);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Blocks.DAYLIGHT_DETECTOR);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDaylightDetector();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(POWER, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWER);
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> items) {
        if (!inverted) super.getSubBlocks(itemIn, items);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
            EnumFacing face) {
        return face == EnumFacing.DOWN ? legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape.SOLID
                : legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape.UNDEFINED;
    }
}
