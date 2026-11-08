package legacyconnector.vb1_7_3.net.minecraft.block.entity;

import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.Inventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtList;

/**
 * Beta 1.7.3 {@code TileEntityChest}: 27 slots (the array has 36, as it had), saved as an {@code Items} list with a {@code Slot} byte. Mods
 * extend it for their own chests (Aether's treasure chests).
 */
public class ChestBlockEntity extends BlockEntity implements Inventory {
    public ItemStack[] inventory = new ItemStack[36];

    public ChestBlockEntity() {}

    @Override
    public int size() {
        return 27;
    }

    @Override
    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < inventory.length ? inventory[slot] : null;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack s = getStack(slot);
        if (s == null) return null;
        ItemStack taken;
        if (s.count <= amount) {
            taken = s;
            inventory[slot] = null;
        } else {
            taken = s.split(amount);
            if (s.count == 0) inventory[slot] = null;
        }
        markDirty();
        return taken;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.length) return;
        inventory[slot] = stack;
        if (stack != null && stack.count > getMaxCountPerStack()) stack.count = getMaxCountPerStack();
        markDirty();
    }

    @Override
    public String getName() {
        return "Chest";
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        NbtList items = nbt.getList("Items");
        inventory = new ItemStack[36];
        for (int i = 0; i < items.size(); i++) {
            NbtCompound item = (NbtCompound) items.get(i);
            int slot = item.getByte("Slot") & 0xFF;
            if (slot < inventory.length) inventory[slot] = new ItemStack(item);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList items = new NbtList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) continue;
            NbtCompound item = new NbtCompound();
            item.putByte("Slot", (byte) i);
            inventory[i].writeNbt(item);
            items.add(item);
        }
        nbt.put("Items", items);
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    /** MCP {@code canInteractWith}: still this chest and the player within 8 blocks. */
    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world != null && world.getBlockEntity(x, y, z) != this) return false;
        return player.getSquaredDistance(x + 0.5, y + 0.5, z + 0.5) <= 64.0;
    }
}
