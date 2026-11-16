package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IShearable {
    boolean isShearable(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z);

    java.util.ArrayList<legacyconnector.v1_7_10.net.minecraft.item.ItemStack> onSheared(legacyconnector.v1_7_10.net.minecraft.item.ItemStack item, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int fortune);
}
