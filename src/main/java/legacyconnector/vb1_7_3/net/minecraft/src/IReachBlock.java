package legacyconnector.vb1_7_3.net.minecraft.src;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
public interface IReachBlock {
    boolean reachBlockItemMatches(ItemStack stack);
    float getReachBlock(ItemStack stack);
}
