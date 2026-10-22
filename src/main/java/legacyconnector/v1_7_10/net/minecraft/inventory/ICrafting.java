package legacyconnector.v1_7_10.net.minecraft.inventory;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

public interface ICrafting {
    void sendContainerAndContentsToPlayer(Container container, List items);

    void sendSlotContents(Container container, int slot, ItemStack stack);

    void sendProgressBarUpdate(Container container, int id, int value);
}
