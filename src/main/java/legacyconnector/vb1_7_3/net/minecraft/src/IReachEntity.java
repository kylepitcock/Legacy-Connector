package legacyconnector.vb1_7_3.net.minecraft.src;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
public interface IReachEntity {
    boolean reachEntityItemMatches(ItemStack stack);
    float getReachEntity(ItemStack stack);
}
