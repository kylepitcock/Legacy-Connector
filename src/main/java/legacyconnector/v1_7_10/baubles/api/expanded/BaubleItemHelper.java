package legacyconnector.v1_7_10.baubles.api.expanded;

import java.util.List;
import legacyconnector.v1_7_10.baubles.api.BaublesApi;
import legacyconnector.v1_7_10.baubles.api.IBauble;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Baubles Expanded's helpers for bauble items: the slot line of the tooltip and equipping by right click. */
public final class BaubleItemHelper {
    private BaubleItemHelper() {}

    /** Adds "Slots: ..." to a tooltip; true when a line was added. */
    public static boolean addSlotInformation(List<String> tooltip, String[] types) {
        if (types == null || types.length == 0) return false;
        StringBuilder sb = new StringBuilder();
        for (String t : types) {
            if (sb.length() > 0) sb.append(", ");
            String key = "slot.baubles." + t;
            String name = StatCollector.translateToLocal(key);
            sb.append(key.equals(name) ? t : name);
        }
        tooltip.add(sb.toString());
        return true;
    }

    /** Puts the held bauble into the first free slot that takes it (server side) and returns the stack. */
    public static ItemStack onBaubleRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world == null || world.isRemote || stack == null || !(stack.getItem() instanceof IBauble bauble)) return stack;
        IInventory baubles = BaublesApi.getBaubles(player);
        if (baubles == null) return stack;
        for (int i = 0; i < baubles.getSizeInventory(); i++) {
            if (baubles.getStackInSlot(i) == null && baubles.isItemValidForSlot(i, stack) && bauble.canEquip(stack, player)) {
                baubles.setInventorySlotContents(i, stack.copy());
                if (!PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities)) {
                    player.inventory.setInventorySlotContents(legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer.mcc$get$currentItem(player.inventory), null);
                }
                bauble.onEquipped(stack, player);
                break;
            }
        }
        return stack;
    }
}
