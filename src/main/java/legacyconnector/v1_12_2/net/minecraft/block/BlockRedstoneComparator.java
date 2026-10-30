package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityComparator;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 comparator: compare or subtract mode (bit 2), powered (bit 3); its output is kept in its tile entity. */
public class BlockRedstoneComparator extends BlockRedstoneDiode implements ITileEntityProvider {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyEnum<Mode> MODE = PropertyEnum.create("mode", Mode.class);

    public BlockRedstoneComparator(boolean powered) {
        super(powered);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false).withProperty(MODE, Mode.COMPARE));
        this.hasTileEntity = true;
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal("item.comparator.name");
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.COMPARATOR;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.COMPARATOR);
    }

    @Override
    protected int getDelay(IBlockState state) {
        return 2;
    }

    @Override
    protected IBlockState getPoweredState(IBlockState unpoweredState) {
        return legacyconnector.v1_12_2.net.minecraft.init.Blocks.POWERED_COMPARATOR.getDefaultState().withProperty(FACING, unpoweredState.getValue(FACING))
                .withProperty(POWERED, unpoweredState.getValue(POWERED)).withProperty(MODE, unpoweredState.getValue(MODE));
    }

    @Override
    protected IBlockState getUnpoweredState(IBlockState poweredState) {
        return legacyconnector.v1_12_2.net.minecraft.init.Blocks.UNPOWERED_COMPARATOR.getDefaultState().withProperty(FACING, poweredState.getValue(FACING))
                .withProperty(POWERED, poweredState.getValue(POWERED)).withProperty(MODE, poweredState.getValue(MODE));
    }

    @Override
    protected boolean isPowered(IBlockState state) {
        return isRepeaterPowered || state.getValue(POWERED);
    }

    @Override
    protected int getActiveSignal(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        return te instanceof TileEntityComparator comparator ? comparator.getOutputSignal() : 0;
    }

    private int calculateOutput(World worldIn, BlockPos pos, IBlockState state) {
        return state.getValue(MODE) == Mode.SUBTRACT ? Math.max(calculateInputStrength(worldIn, pos, state) - getPowerOnSides(worldIn, pos, state), 0)
                : calculateInputStrength(worldIn, pos, state);
    }

    @Override
    protected boolean shouldBePowered(World worldIn, BlockPos pos, IBlockState state) {
        int input = calculateInputStrength(worldIn, pos, state);
        if (input >= 15) return true;
        if (input == 0) return false;
        int sides = getPowerOnSides(worldIn, pos, state);
        return sides == 0 || input >= sides;
    }

    /** A container (or other comparator source) behind it counts as input. */
    @Override
    protected int calculateInputStrength(World worldIn, BlockPos pos, IBlockState state) {
        int i = super.calculateInputStrength(worldIn, pos, state);
        EnumFacing facing = state.getValue(FACING);
        BlockPos back = pos.offset(facing);
        IBlockState backState = worldIn.getBlockState(back);
        if (backState.hasComparatorInputOverride()) i = backState.getComparatorInputOverride(worldIn, back);
        return i;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (!playerIn.isAllowEdit()) return false;
        state = state.cycleProperty(MODE);
        worldIn.setBlockState(pos, state, 2);
        onStateChange(worldIn, pos, state);
        return true;
    }

    private void onStateChange(World worldIn, BlockPos pos, IBlockState state) {
        int output = calculateOutput(worldIn, pos, state);
        TileEntity te = worldIn.getTileEntity(pos);
        int old = 0;
        if (te instanceof TileEntityComparator comparator) {
            old = comparator.getOutputSignal();
            comparator.setOutputSignal(output);
        }
        if (old != output || state.getValue(MODE) == Mode.COMPARE) {
            boolean should = shouldBePowered(worldIn, pos, state);
            boolean is = isPowered(state);
            if (is && !should) worldIn.setBlockState(pos, state.withProperty(POWERED, false), 2);
            else if (!is && should) worldIn.setBlockState(pos, state.withProperty(POWERED, true), 2);
            notifyNeighbors(worldIn, pos, state);
        }
    }

    @Override
    protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isBlockTickPending(pos, this)) worldIn.scheduleUpdate(pos, this, 2);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        onStateChange(worldIn, pos, state);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        worldIn.setTileEntity(pos, createNewTileEntity(worldIn, 0));
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
        notifyNeighbors(worldIn, pos, state);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityComparator();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta)).withProperty(POWERED, (meta & 8) > 0)
                .withProperty(MODE, (meta & 4) > 0 ? Mode.SUBTRACT : Mode.COMPARE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(POWERED)) i |= 8;
        if (state.getValue(MODE) == Mode.SUBTRACT) i |= 4;
        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, MODE, POWERED);
    }

    public enum Mode implements IStringSerializable {
        COMPARE("compare"),
        SUBTRACT("subtract");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
