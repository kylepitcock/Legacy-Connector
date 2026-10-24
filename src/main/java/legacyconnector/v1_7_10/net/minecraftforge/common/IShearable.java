package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.ArrayList;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** Blocks and entities that shears can harvest: they say whether they can be sheared and what they give. */
public interface IShearable {
    boolean isShearable(ItemStack item, IBlockAccess world, int x, int y, int z);

    ArrayList<ItemStack> onSheared(ItemStack item, IBlockAccess world, int x, int y, int z, int fortune);
}
