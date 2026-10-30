package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/** 1.12.2's creative inventory listener: every slot change of the player's container is sent to the server as a creative slot set. */
public class CreativeCrafting implements IContainerListener {
    private final Minecraft mc;

    public CreativeCrafting(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        if (mc != null && mc.playerController != null) mc.playerController.sendSlotPacket(stack, slotInd);
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
    }

    @Override
    public void sendAllWindowProperties(Container containerIn, IInventory inventory) {
    }
}
