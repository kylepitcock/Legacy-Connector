package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.block.state.BlockStateContainer;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 monster egg (infested stone): looks like a stone block, breaks instantly and releases a silverfish. */
public class BlockSilverfish extends Block {
    public static final PropertyEnum<EnumType> VARIANT = PropertyEnum.create("variant", EnumType.class);

    public BlockSilverfish() {
        super(Material.CLAY);
        setDefaultState(blockState.getBaseState().withProperty(VARIANT, EnumType.STONE));
        setHardness(0.0F);
        setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    /** Whether a silverfish can hide in a block (stone, cobblestone and stone bricks). */
    public static boolean canContainSilverfish(IBlockState blockState) {
        Block block = blockState.getBlock();
        return blockState == Blocks.STONE.getDefaultState() || block == Blocks.COBBLESTONE || block == Blocks.STONEBRICK;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        IBlockState model = state.getValue(VARIANT).getModelBlock();
        return new ItemStack(model.getBlock(), 1, model.getBlock().getMetaFromState(model));
    }

    /** Broken without silk touch: a silverfish comes out (the game's silverfish). */
    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        var level = worldIn.mcc$level();
        if (level instanceof net.minecraft.server.level.ServerLevel server && server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) {
            var fish = net.minecraft.world.entity.EntityType.SILVERFISH.create(server);
            if (fish != null) {
                fish.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                server.addFreshEntity(fish);
                fish.spawnAnim();
            }
        }
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(this, 1, state.getBlock().getMetaFromState(state));
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (EnumType t : EnumType.values()) items.add(new ItemStack(this, 1, t.getMetadata()));
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, EnumType.byMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, VARIANT);
    }

    public enum EnumType implements IStringSerializable {
        STONE(0, "stone", "stone"),
        COBBLESTONE(1, "cobblestone", "cobble"),
        STONEBRICK(2, "stone_brick", "brick"),
        MOSSY_STONEBRICK(3, "mossy_brick", "mossybrick"),
        CRACKED_STONEBRICK(4, "cracked_brick", "crackedbrick"),
        CHISELED_STONEBRICK(5, "chiseled_brick", "chiseledbrick");

        private static final EnumType[] META_LOOKUP = new EnumType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;

        EnumType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return meta;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EnumType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) meta = 0;
            return META_LOOKUP[meta];
        }

        @Override
        public String getName() {
            return name;
        }

        public String getTranslationKey() {
            return unlocalizedName;
        }

        public String getUnlocalizedName() {
            return unlocalizedName;
        }

        /** The block this variant looks like. */
        public IBlockState getModelBlock() {
            switch (this) {
                case COBBLESTONE:
                    return Blocks.COBBLESTONE.getDefaultState();
                case STONEBRICK:
                    return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.DEFAULT);
                case MOSSY_STONEBRICK:
                    return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.MOSSY);
                case CRACKED_STONEBRICK:
                    return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
                case CHISELED_STONEBRICK:
                    return Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED);
                default:
                    return Blocks.STONE.getDefaultState();
            }
        }

        /** The variant that hides in a block, STONE for any other. */
        public static EnumType forModelBlock(IBlockState model) {
            for (EnumType t : values()) if (model == t.getModelBlock()) return t;
            return STONE;
        }

        static {
            for (EnumType t : values()) META_LOOKUP[t.getMetadata()] = t;
        }
    }
}
