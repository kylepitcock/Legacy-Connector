package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Shears: fast on leaves, wool and webs; harvest webs, redstone wire and tripwire; wear by one per block. */
public class ItemShears extends Item {
    public ItemShears() {
        setMaxStackSize(1);
        setMaxDamage(238);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.TOOLS);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, IBlockState state, BlockPos pos, EntityLivingBase entityLiving) {
        if (!worldIn.isRemote) stack.damageItem(1, entityLiving);
        var b = state.getBlock();
        return state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LEAVES
                || legacyconnector.v1_12_2.net.minecraft.block.Block.mcc$named(b, "web", "tallgrass", "vine", "tripwire", "wool")
                || super.onBlockDestroyed(stack, worldIn, state, pos, entityLiving);
    }

    @Override
    public boolean canHarvestBlock(IBlockState blockIn) {
        return legacyconnector.v1_12_2.net.minecraft.block.Block.mcc$named(blockIn.getBlock(), "web", "redstone_wire", "tripwire");
    }

    @Override
    public float getDestroySpeed(ItemStack stack, IBlockState state) {
        var b = state.getBlock();
        if (legacyconnector.v1_12_2.net.minecraft.block.Block.mcc$named(b, "web") || state.getMaterial() == legacyconnector.v1_12_2.net.minecraft.block.material.Material.LEAVES) {
            return 15.0F;
        }
        return legacyconnector.v1_12_2.net.minecraft.block.Block.mcc$named(b, "wool") ? 5.0F : super.getDestroySpeed(stack, state);
    }
}
