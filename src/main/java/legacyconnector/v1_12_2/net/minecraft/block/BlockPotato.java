package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** 1.12.2 potatoes: a crop of potatoes; a ripe one sometimes also drops a poisonous potato (2%). */
public class BlockPotato extends BlockCrops {
    @Override
    protected Item getSeed() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.POTATO;
    }

    @Override
    protected Item getCrop() {
        return legacyconnector.v1_12_2.net.minecraft.init.Items.POTATO;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        super.getDrops(drops, world, pos, state, fortune);
        Random rand = world instanceof legacyconnector.v1_12_2.net.minecraft.world.World w ? w.rand : new Random();
        if (isMaxAge(state) && rand.nextInt(50) == 0) drops.add(new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.POISONOUS_POTATO));
    }
}
