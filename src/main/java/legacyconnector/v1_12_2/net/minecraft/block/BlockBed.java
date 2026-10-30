package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.MapColor;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityBed;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.Mirror;
import legacyconnector.v1_12_2.net.minecraft.util.Rotation;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 beds: two blocks (foot and head), facing bits 0-1, occupied bit 2, head bit 3; the colour lives in the tile entity. Sleeping
 * itself is the modern bed's; a mod's bed is slept in through the player's trySleep.
 */
public class BlockBed extends BlockHorizontal implements ITileEntityProvider {
    public static final PropertyEnum<EnumPartType> PART = PropertyEnum.create("part", EnumPartType.class);
    public static final PropertyBool OCCUPIED = PropertyBool.create("occupied");
    protected static final AxisAlignedBB BED_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5625D, 1.0D);

    public BlockBed() {
        super(Material.CLOTH);
        setDefaultState(blockState.getBaseState().withProperty(PART, EnumPartType.FOOT).withProperty(OCCUPIED, false));
        this.hasTileEntity = true;
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (state.getValue(PART) == EnumPartType.FOOT) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityBed bed) return MapColor.getBlockColor(bed.getColor());
        }
        return MapColor.CLOTH;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX,
            float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        if (state.getValue(PART) != EnumPartType.HEAD) {
            pos = pos.offset(state.getValue(FACING));
            state = worldIn.getBlockState(pos);
            if (state.getBlock() != this) return true;
        }
        if (World.mcc$get$provider(worldIn) != null && !World.mcc$get$provider(worldIn).canRespawnHere()) {
            worldIn.setBlockToAir(pos);
            BlockPos foot = pos.offset(state.getValue(FACING).getOpposite());
            if (worldIn.getBlockState(foot).getBlock() == this) worldIn.setBlockToAir(foot);
            worldIn.newExplosion(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5.0F, true, true);
            return true;
        }
        playerIn.trySleep(pos);
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    /** Half a bed alone breaks. */
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        EnumFacing facing = state.getValue(FACING);
        if (state.getValue(PART) == EnumPartType.FOOT) {
            if (worldIn.getBlockState(pos.offset(facing)).getBlock() != this) worldIn.setBlockToAir(pos);
        } else if (worldIn.getBlockState(pos.offset(facing.getOpposite())).getBlock() != this) {
            if (!worldIn.isRemote) dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return state.getValue(PART) == EnumPartType.FOOT ? null : legacyconnector.v1_12_2.net.minecraft.init.Items.BED;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BED_AABB;
    }

    @Override
    public boolean hasCustomBreakingProgress(IBlockState state) {
        return true;
    }

    /** The bed's colour as the dropped item's metadata. */
    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (state.getValue(PART) == EnumPartType.HEAD) {
            TileEntity te = worldIn.getTileEntity(pos);
            EnumDyeColor color = te instanceof TileEntityBed bed ? bed.getColor() : EnumDyeColor.RED;
            spawnAsEntity(worldIn, pos, new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BED, 1, color.getMetadata()));
        }
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction getPushReaction(IBlockState state) {
        return legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction.DESTROY;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        BlockPos head = state.getValue(PART) == EnumPartType.HEAD ? pos : pos.offset(state.getValue(FACING));
        TileEntity te = worldIn.getTileEntity(head);
        EnumDyeColor color = te instanceof TileEntityBed bed ? bed.getColor() : EnumDyeColor.RED;
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BED, 1, color.getMetadata());
    }

    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (player.isCreative() && state.getValue(PART) == EnumPartType.FOOT) {
            BlockPos head = pos.offset(state.getValue(FACING));
            if (worldIn.getBlockState(head).getBlock() == this) worldIn.setBlockToAir(head);
        }
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
        if (state.getValue(PART) == EnumPartType.HEAD && te instanceof TileEntityBed bed) {
            spawnAsEntity(worldIn, pos, new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BED, 1, bed.getColor().getMetadata()));
        } else {
            super.harvestBlock(worldIn, player, pos, state, null, stack);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta);
        return (meta & 8) > 0 ? getDefaultState().withProperty(PART, EnumPartType.HEAD).withProperty(FACING, facing).withProperty(OCCUPIED, (meta & 4) > 0)
                : getDefaultState().withProperty(PART, EnumPartType.FOOT).withProperty(FACING, facing);
    }

    /** The foot reads OCCUPIED from its head. */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        if (state.getValue(PART) == EnumPartType.FOOT) {
            IBlockState head = worldIn.getBlockState(pos.offset(state.getValue(FACING)));
            if (head.getBlock() == this) state = state.withProperty(OCCUPIED, head.getValue(OCCUPIED));
        }
        return state;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
        return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(PART) == EnumPartType.HEAD) {
            i |= 8;
            if (state.getValue(OCCUPIED)) i |= 4;
        }
        return i;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, PART, OCCUPIED);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBed();
    }

    /** Whether the bed block at a position is the head (static form 1.12 mods call). */
    public static boolean isHeadPiece(int metadata) {
        return (metadata & 8) != 0;
    }

    public enum EnumPartType implements IStringSerializable {
        HEAD("head"),
        FOOT("foot");

        private final String name;

        EnumPartType(String name) {
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
