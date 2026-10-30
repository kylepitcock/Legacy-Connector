package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 repeater: delay 1-4 ticks (bits 2-3), locked by a powered repeater beside it; right-click cycles the delay. */
public class BlockRedstoneRepeater extends BlockRedstoneDiode {
    public static final PropertyBool LOCKED = PropertyBool.create("locked");
    public static final PropertyInteger DELAY = PropertyInteger.create("delay", 1, 4);

    protected BlockRedstoneRepeater(boolean powered) {
        super(powered);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(DELAY, 1).withProperty(LOCKED, false));
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal("item.diode.name");
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.withProperty(LOCKED, isLocked(worldIn, pos, state));
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (!playerIn.isAllowEdit()) return false;
        worldIn.setBlockState(pos, state.cycleProperty(DELAY), 3);
        return true;
    }

    @Override
    protected int getDelay(IBlockState state) {
        return state.getValue(DELAY) * 2;
    }

    @Override
    protected IBlockState getPoweredState(IBlockState unpoweredState) {
        return legacyconnector.v1_12_2.net.minecraft.init.Blocks.POWERED_REPEATER.getDefaultState().withProperty(FACING, unpoweredState.getValue(FACING))
                .withProperty(DELAY, unpoweredState.getValue(DELAY)).withProperty(LOCKED, unpoweredState.getValue(LOCKED));
    }

    @Override
    protected IBlockState getUnpoweredState(IBlockState poweredState) {
        return legacyconnector.v1_12_2.net.minecraft.init.Blocks.UNPOWERED_REPEATER.getDefaultState().withProperty(FACING, poweredState.getValue(FACING))
                .withProperty(DELAY, poweredState.getValue(DELAY)).withProperty(LOCKED, poweredState.getValue(LOCKED));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.REPEATER;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.REPEATER);
    }

    @Override
    public boolean isLocked(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
        return getPowerOnSides(worldIn, pos, state) > 0;
    }

    @Override
    protected boolean isAlternateInput(IBlockState state) {
        return isDiode(state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta)).withProperty(LOCKED, false).withProperty(DELAY, 1 + (meta >> 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(DELAY) - 1 << 2;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, DELAY, LOCKED);
    }
}
