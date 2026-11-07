package legacyconnector.v1_12_2.net.minecraftforge.items.wrapper;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Forge's {@code EntityArmorInvWrapper} / {@code EntityHandsInvWrapper}: the equipment slots of one type of a living entity as an item
 * handler (armour: boots first; hands: main hand, off hand). Forge 1.12.2 gave every living entity these through
 * {@code getCapability(ITEM_HANDLER_CAPABILITY, facing)}: up = hands, a side = armour, null = both.
 */
public class EntityEquipmentInvWrapper implements IItemHandlerModifiable {
    private final EntityLivingBase entity;
    private final EntityEquipmentSlot[] slots;

    public EntityEquipmentInvWrapper(EntityLivingBase entity, EntityEquipmentSlot.Type type) {
        this.entity = entity;
        java.util.List<EntityEquipmentSlot> list = new java.util.ArrayList<>();
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) if (s.getSlotType() == type) list.add(s);
        this.slots = list.toArray(new EntityEquipmentSlot[0]);
    }

    @Override
    public int getSlots() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot < 0 || slot >= slots.length ? ItemStack.EMPTY : entity.getItemStackFromSlot(slots[slot]);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < slots.length) entity.setItemStackToSlot(slots[slot], stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= slots.length) return stack;
        EntityEquipmentSlot equ = slots[slot];
        if (equ.getSlotType() == EntityEquipmentSlot.Type.ARMOR && !stack.getItem().isValidArmor(stack, equ, entity)) return stack;
        ItemStack existing = entity.getItemStackFromSlot(equ);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.areItemsEqual(stack, existing) || !ItemStack.areItemStackTagsEqual(stack, existing)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                entity.setItemStackToSlot(equ, reachedLimit ? stack.splitStack(limit) : stack.copy());
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
        }
        if (reachedLimit) {
            ItemStack rest = stack.copy();
            rest.shrink(limit);
            return rest;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0 || slot < 0 || slot >= slots.length) return ItemStack.EMPTY;
        EntityEquipmentSlot equ = slots[slot];
        ItemStack existing = entity.getItemStackFromSlot(equ);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (!simulate) entity.setItemStackToSlot(equ, ItemStack.EMPTY);
            return existing;
        }
        if (!simulate) return existing.splitStack(toExtract);
        ItemStack copy = existing.copy();
        copy.setCount(toExtract);
        return copy;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot >= 0 && slot < slots.length && slots[slot].getSlotType() == EntityEquipmentSlot.Type.ARMOR ? 1 : 64;
    }
}
