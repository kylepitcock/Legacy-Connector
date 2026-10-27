package legacyconnector.v1_12_2.net.minecraftforge.items;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public class ItemHandlerHelper {
    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;
        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        return stack;
    }

    public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
        if (a.isEmpty() || !a.isItemEqual(b) || a.hasTagCompound() != b.hasTagCompound()) return false;
        return !a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound());
    }

    /** Like canItemStacksStack, but also for stacks that are not stackable themselves (the target may hold one more). */
    public static boolean canItemStacksStackRelaxed(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;
        if (!a.isStackable()) return false;
        if (a.getHasSubtypes() && a.getMetadata() != b.getMetadata()) return false;
        if (a.hasTagCompound() != b.hasTagCompound()) return false;
        return !a.hasTagCompound() || a.getTagCompound().equals(b.getTagCompound());
    }

    /** Inserts onto matching stacks first, then into empty slots (or anywhere for unstackable items). */
    public static ItemStack insertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || stack.isEmpty()) return stack;
        if (!stack.isStackable()) return insertItem(inventory, stack, simulate);
        int sizeInventory = inventory.getSlots();
        for (int i = 0; i < sizeInventory; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (canItemStacksStackRelaxed(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) break;
            }
        }
        if (!stack.isEmpty()) {
            for (int i = 0; i < sizeInventory; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack.isEmpty()) break;
                }
            }
        }
        return stack;
    }

    /** Puts the stack into the player's inventory, dropping what does not fit at the player (with the pickup sound). */
    public static void giveItemToPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, ItemStack stack) {
        giveItemToPlayer(player, stack, -1);
    }

    public static void giveItemToPlayer(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, ItemStack stack, int preferredSlot) {
        if (stack.isEmpty() || player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)) return;
        net.neoforged.neoforge.items.ItemHandlerHelper.giveItemToPlayer(p, org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack), preferredSlot);
    }

    /** The comparator signal of an item handler's fill level. */
    public static int calcRedstoneFromInventory(IItemHandler inv) {
        if (inv == null) return 0;
        int itemsFound = 0;
        float proportion = 0.0F;
        for (int j = 0; j < inv.getSlots(); j++) {
            ItemStack itemstack = inv.getStackInSlot(j);
            if (!itemstack.isEmpty()) {
                proportion += (float) itemstack.getCount() / (float) Math.min(inv.getSlotLimit(j), itemstack.getMaxStackSize());
                itemsFound++;
            }
        }
        proportion /= (float) inv.getSlots();
        return (int) Math.floor(proportion * 14.0F) + (itemsFound > 0 ? 1 : 0);
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size == 0) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(size);
        return copy;
    }
}
