package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntityBeacon;

/** The beacon container: one payment slot (0), then the player's inventory (1-27) and hotbar (28-36). */
public class ContainerBeacon extends Container {
    private final TileEntityBeacon tileBeacon;

    public ContainerBeacon(InventoryPlayer inventory, TileEntityBeacon beacon) {
        this.tileBeacon = beacon;
        addSlotToContainer(new Slot(beacon, 0, 136, 110) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack != null && stack.getItem() != null && stack.getItem().isBeaconPayment(stack);
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 36 + col * 18, 137 + row * 18));
        }
        for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col, 36 + col * 18, 195));
    }

    public TileEntityBeacon func_148327_e() {
        return tileBeacon;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tileBeacon.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return null;
    }
}
