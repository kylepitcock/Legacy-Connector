package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyDirection;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 anvils: a falling block facing sideways with three damage stages; right-click opens the modern anvil screen, which wears the
 * mod's anvil (12% per use, as vanilla's) instead of only vanilla anvils.
 */
public class BlockAnvil extends BlockFalling {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger DAMAGE = PropertyInteger.create("damage", 0, 2);
    protected static final AxisAlignedBB X_AXIS_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.125D, 1.0D, 1.0D, 0.875D);
    protected static final AxisAlignedBB Z_AXIS_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.0D, 0.875D, 1.0D, 1.0D);

    protected BlockAnvil() {
        super(Material.ANVIL);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(DAMAGE, 0));
        setLightOpacity(0);
        setCreativeTab(CreativeTabs.DECORATIONS);
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        EnumFacing enumfacing = placer.getHorizontalFacing().rotateY();
        return getDefaultState().withProperty(FACING, enumfacing).withProperty(DAMAGE, Math.min(2, meta >> 2));
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        var player = playerIn.mcc$player();
        var level = worldIn.mcc$level();
        if (player == null || level == null) return true;
        var mp = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos);
        var anvil = level.getBlockState(mp).getBlock();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new net.minecraft.world.inventory.AnvilMenu(id, inventory,
                net.minecraft.world.inventory.ContainerLevelAccess.create(level, mp)) {
            @Override
            protected boolean isValidBlock(net.minecraft.world.level.block.state.BlockState s) {
                return s.is(anvil);
            }

            @Override
            protected void onTake(net.minecraft.world.entity.player.Player who, net.minecraft.world.item.ItemStack stack) {
                super.onTake(who, stack);
                if (who.getAbilities().instabuild || who.getRandom().nextFloat() >= 0.12F) return;
                IBlockState now = worldIn.getBlockState(pos);
                if (now.getBlock() != BlockAnvil.this) return;
                int damage = now.getValue(DAMAGE) + 1;
                if (damage > 2) {
                    worldIn.setBlockToAir(pos);
                    level.levelEvent(1029, mp, 0);
                } else {
                    worldIn.setBlockState(pos, now.withProperty(DAMAGE, damage), 2);
                }
            }
        }, net.minecraft.network.chat.Component.translatable("container.repair")));
        return true;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(DAMAGE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return mcc$staticBounds(state);
    }

    @Override
    public AxisAlignedBB mcc$staticBounds(IBlockState state) {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
        items.add(new ItemStack(this, 1, 1));
        items.add(new ItemStack(this, 1, 2));
    }

    @Override
    public void onEndFalling(World worldIn, BlockPos pos, IBlockState fallingState, IBlockState hitState) {
        worldIn.playEvent(1031, pos, 0);
    }

    @Override
    public void onBroken(World worldIn, BlockPos pos) {
        worldIn.playEvent(1029, pos, 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(DAMAGE, Math.min(2, (meta & 15) >> 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(DAMAGE) << 2;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.getBlock() != this ? state : state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, DAMAGE);
    }
}
