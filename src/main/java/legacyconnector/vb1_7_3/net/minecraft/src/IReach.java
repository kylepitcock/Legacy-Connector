package legacyconnector.vb1_7_3.net.minecraft.src;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
/** Original ShockAhPI held-item reach contract. */
public interface IReach {
    boolean reachItemMatches(ItemStack stack);
    float getReach(ItemStack stack);
}
