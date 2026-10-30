package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;

/** 1.12.2 clay block: drops four clay balls. */
public class BlockClay extends Block {
    public BlockClay() {
        super(Material.CLAY);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.CLAY_BALL;
    }

    @Override
    public int quantityDropped(Random random) {
        return 4;
    }
}
