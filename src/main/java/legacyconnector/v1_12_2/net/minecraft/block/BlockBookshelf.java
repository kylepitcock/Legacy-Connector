package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 bookshelf: drops three books, powers enchanting tables (Forge's enchant power 1). */
public class BlockBookshelf extends Block {
    public BlockBookshelf() {
        super(Material.WOOD);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public int quantityDropped(Random random) {
        return 3;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.BOOK;
    }

    @Override
    public float getEnchantPowerBonus(World world, BlockPos pos) {
        return 1.0F;
    }
}
