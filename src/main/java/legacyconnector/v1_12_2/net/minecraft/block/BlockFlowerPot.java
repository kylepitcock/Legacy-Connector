package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyInteger;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockFaceShape;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityFlowerPot;
import legacyconnector.v1_12_2.net.minecraft.util.BlockRenderLayer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumBlockRenderType;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.math.AxisAlignedBB;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 flower pots: the plant is the tile entity's item (CONTENTS is its actual state, read from that item); the metadata is only the
 * legacy data value.
 */
public class BlockFlowerPot extends BlockContainer {
    public static final PropertyInteger LEGACY_DATA = PropertyInteger.create("legacy_data", 0, 15);
    public static final PropertyEnum<EnumFlowerType> CONTENTS = PropertyEnum.create("contents", EnumFlowerType.class);
    protected static final AxisAlignedBB FLOWER_POT_AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.375D, 0.6875D);

    public BlockFlowerPot() {
        super(Material.CIRCUITS);
        setDefaultState(blockState.getBaseState().withProperty(CONTENTS, EnumFlowerType.EMPTY).withProperty(LEGACY_DATA, 0));
    }

    @Override
    public String getLocalizedName() {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal("item.flowerPot.name");
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FLOWER_POT_AABB;
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
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        IBlockState below = worldIn.getBlockState(pos.down());
        return super.canPlaceBlockAt(worldIn, pos) && (below.isTopSolid() || below.getBlockFaceShape(worldIn, pos.down(), EnumFacing.UP) == BlockFaceShape.SOLID);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        IBlockState below = worldIn.getBlockState(pos.down());
        if (!below.isTopSolid() && below.getBlockFaceShape(worldIn, pos.down(), EnumFacing.UP) != BlockFaceShape.SOLID) {
            dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    /** The pot itself, or the plant in it when picked. */
    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        TileEntityFlowerPot pot = getTileEntity(worldIn, pos);
        if (pot != null) {
            ItemStack flower = pot.getFlowerItemStack();
            if (!flower.isEmpty()) return flower;
        }
        return new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.FLOWER_POT);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.FLOWER_POT;
    }

    private TileEntityFlowerPot getTileEntity(IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        return te instanceof TileEntityFlowerPot pot ? pot : null;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFlowerPot();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CONTENTS, LEGACY_DATA);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEGACY_DATA);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEGACY_DATA, meta & 15);
    }

    /** The plant in the pot as the CONTENTS value. */
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        EnumFlowerType type = EnumFlowerType.EMPTY;
        TileEntityFlowerPot pot = getTileEntity(worldIn, pos);
        if (pot != null) {
            Item item = pot.getFlowerPotItem();
            int data = pot.getFlowerPotData();
            if (item != null && item != legacyconnector.v1_12_2.net.minecraft.init.Items.AIR) type = EnumFlowerType.of(item, data);
        }
        return state.withProperty(CONTENTS, type);
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    public enum EnumFlowerType implements IStringSerializable {
        EMPTY("empty"),
        POPPY("rose"),
        BLUE_ORCHID("blue_orchid"),
        ALLIUM("allium"),
        HOUSTONIA("houstonia"),
        RED_TULIP("red_tulip"),
        ORANGE_TULIP("orange_tulip"),
        WHITE_TULIP("white_tulip"),
        PINK_TULIP("pink_tulip"),
        OXEYE_DAISY("oxeye_daisy"),
        DANDELION("dandelion"),
        OAK_SAPLING("oak_sapling"),
        SPRUCE_SAPLING("spruce_sapling"),
        BIRCH_SAPLING("birch_sapling"),
        JUNGLE_SAPLING("jungle_sapling"),
        ACACIA_SAPLING("acacia_sapling"),
        DARK_OAK_SAPLING("dark_oak_sapling"),
        MUSHROOM_RED("mushroom_red"),
        MUSHROOM_BROWN("mushroom_brown"),
        DEAD_BUSH("dead_bush"),
        FERN("fern"),
        CACTUS("cactus");

        private final String name;

        EnumFlowerType(String name) {
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

        /** The kind of a potted item and data value, as 1.12's pot reads them. */
        static EnumFlowerType of(Item item, int data) {
            Block block = Block.getBlockFromItem(item);
            if (block != null) {
                if (Block.mcc$named(block, "red_flower")) {
                    EnumFlowerType[] red = {POPPY, BLUE_ORCHID, ALLIUM, HOUSTONIA, RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP, OXEYE_DAISY};
                    return data >= 0 && data < red.length ? red[data] : EMPTY;
                }
                if (Block.mcc$named(block, "yellow_flower")) return DANDELION;
                if (Block.mcc$named(block, "sapling")) {
                    EnumFlowerType[] saplings = {OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING, JUNGLE_SAPLING, ACACIA_SAPLING, DARK_OAK_SAPLING};
                    return data >= 0 && data < saplings.length ? saplings[data] : EMPTY;
                }
                if (Block.mcc$named(block, "red_mushroom")) return MUSHROOM_RED;
                if (Block.mcc$named(block, "brown_mushroom")) return MUSHROOM_BROWN;
                if (Block.mcc$named(block, "deadbush")) return DEAD_BUSH;
                if (Block.mcc$named(block, "cactus")) return CACTUS;
                if (Block.mcc$named(block, "tallgrass")) return data == 2 ? FERN : data == 0 ? DEAD_BUSH : EMPTY;
            }
            return EMPTY;
        }
    }
}
