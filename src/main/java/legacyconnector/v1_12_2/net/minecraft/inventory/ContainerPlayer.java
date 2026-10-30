package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/**
 * 1.12.2's survival inventory container: the 2x2 crafting grid and its result, the armour slots, the main inventory, the hotbar and the
 * offhand (slots 0-45 as 1.12 numbered them). Mods subclass it for containers that keep the player's inventory layout (backpacks,
 * accessory screens); the player's own inventory is the modern inventory menu.
 */
public class ContainerPlayer extends Container {
    private static final EntityEquipmentSlot[] VALID_EQUIPMENT_SLOTS = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS,
            EntityEquipmentSlot.FEET};
    public InventoryCrafting craftMatrix = new InventoryCrafting(this, 2, 2);
    public InventoryCraftResult craftResult = new InventoryCraftResult();
    public boolean isLocalWorld;
    private final EntityPlayer player;

    public ContainerPlayer(InventoryPlayer playerInventory, boolean localWorld, EntityPlayer playerIn) {
        this.isLocalWorld = localWorld;
        this.player = playerIn;
        addSlotToContainer(new SlotCrafting(playerInventory.player, craftMatrix, craftResult, 0, 154, 28));
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) addSlotToContainer(new Slot(craftMatrix, j + i * 2, 98 + j * 18, 18 + i * 18));
        }
        for (int k = 0; k < 4; ++k) {
            final EntityEquipmentSlot slot = VALID_EQUIPMENT_SLOTS[k];
            addSlotToContainer(new Slot(playerInventory, 36 + (3 - k), 8, 8 + k * 18) {
                @Override
                public int getSlotStackLimit() {
                    return 1;
                }

                @Override
                public boolean isItemValid(ItemStack stack) {
                    return !stack.isEmpty() && stack.getItem().isValidArmor(stack, slot, player)
                            || !stack.isEmpty() && EntityLiving.getSlotForItemStack(stack) == slot;
                }

                @Override
                public boolean canTakeStack(EntityPlayer playerIn) {
                    ItemStack stack = getStack();
                    return !stack.isEmpty() && !playerIn.isCreative()
                            && legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.hasBindingCurse(stack) ? false : super.canTakeStack(playerIn);
                }

                @Override
                public String getSlotTexture() {
                    return legacyconnector.v1_12_2.net.minecraft.item.ItemArmor.EMPTY_SLOT_NAMES[slot.getIndex()];
                }
            });
        }
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) addSlotToContainer(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
        }
        for (int i1 = 0; i1 < 9; ++i1) addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
        addSlotToContainer(new Slot(playerInventory, 40, 77, 62) {
            @Override
            public String getSlotTexture() {
                return "minecraft:items/empty_armor_slot_shield";
            }
        });
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        if (player != null) slotChangedCraftingGrid(player.getEntityWorld(), player, craftMatrix, craftResult);
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        craftResult.clear();
        if (!isLocalWorld && playerIn != null) clearContainer(playerIn, playerIn.getEntityWorld(), craftMatrix);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /** Shift-click: the result to the inventory, armour to its slot, the offhand item to the offhand, between inventory and hotbar. */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            EntityEquipmentSlot equipment = EntityLiving.getSlotForItemStack(itemstack);
            if (index == 0) {
                if (!mergeItemStack(itemstack1, 9, 45, true)) return ItemStack.EMPTY;
                slot.onSlotChange(itemstack1, itemstack);
            } else if (index >= 1 && index < 5) {
                if (!mergeItemStack(itemstack1, 9, 45, false)) return ItemStack.EMPTY;
            } else if (index >= 5 && index < 9) {
                if (!mergeItemStack(itemstack1, 9, 45, false)) return ItemStack.EMPTY;
            } else if (equipment.getSlotType() == EntityEquipmentSlot.Type.ARMOR && !inventorySlots.get(8 - equipment.getIndex()).getHasStack()) {
                int i = 8 - equipment.getIndex();
                if (!mergeItemStack(itemstack1, i, i + 1, false)) return ItemStack.EMPTY;
            } else if (equipment == EntityEquipmentSlot.OFFHAND && !inventorySlots.get(45).getHasStack()) {
                if (!mergeItemStack(itemstack1, 45, 46, false)) return ItemStack.EMPTY;
            } else if (index >= 9 && index < 36) {
                if (!mergeItemStack(itemstack1, 36, 45, false)) return ItemStack.EMPTY;
            } else if (index >= 36 && index < 45) {
                if (!mergeItemStack(itemstack1, 9, 36, false)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(itemstack1, 9, 45, false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);
            if (index == 0) org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.dropFromPlayer(playerIn, itemstack2);
        }
        return itemstack;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return slotIn.inventory != craftResult && super.canMergeSlot(stack, slotIn);
    }

    public InventoryCraftResult getCraftResult() {
        return craftResult;
    }

    public InventoryCrafting getCraftMatrix() {
        return craftMatrix;
    }
}
