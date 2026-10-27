package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/** A block or entity shears can harvest (leaves, vines, sheep): whether it can be sheared now, and what shearing drops. */
public interface IShearable {
    boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos);

    List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune);
}
