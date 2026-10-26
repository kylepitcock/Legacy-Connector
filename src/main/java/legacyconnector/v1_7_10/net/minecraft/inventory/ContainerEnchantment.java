package legacyconnector.v1_7_10.net.minecraft.inventory;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentData;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.ForgeHooks;

/**
 * The 1.7.10 enchanting table container, ported from vanilla: one item slot, then the player's inventory (1-27) and hotbar
 * (28-36). The three offered levels come from the bookshelf power around the table and are synced as progress bars 0-2;
 * {@link #enchantItem} costs the whole level, as in 1.7.10.
 */
public class ContainerEnchantment extends Container {
    public IInventory tableInventory = new InventoryBasic("Enchant", true, 1) {
        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            super.markDirty();
            ContainerEnchantment.this.onCraftMatrixChanged(this);
        }
    };
    private World worldPointer;
    private int posX;
    private int posY;
    private int posZ;
    private Random rand = new Random();
    public long nameSeed;
    public int[] enchantLevels = new int[3];

    public ContainerEnchantment(InventoryPlayer inventory, World world, int x, int y, int z) {
        this.worldPointer = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        addSlotToContainer(new Slot(tableInventory, 0, 25, 47) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return true;
            }
        });
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; ++col) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        for (int i = 0; i < 3; ++i) crafter.sendProgressBarUpdate(this, i, enchantLevels[i]);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Object o : crafters) {
            for (int i = 0; i < 3; ++i) ((ICrafting) o).sendProgressBarUpdate(this, i, enchantLevels[i]);
        }
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id >= 0 && id <= 2) enchantLevels[id] = value;
        else super.updateProgressBar(id, value);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inv) {
        if (inv != tableInventory) return;
        ItemStack stack = inv.getStackInSlot(0);
        if (stack != null && stack.isItemEnchantable()) {
            nameSeed = rand.nextLong();
            if (!worldPointer.isRemote) {
                float power = 0.0F;
                for (int dz = -1; dz <= 1; ++dz) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        if ((dz != 0 || dx != 0) && worldPointer.isAirBlock(posX + dx, posY, posZ + dz) && worldPointer.isAirBlock(posX + dx, posY + 1, posZ + dz)) {
                            power += ForgeHooks.getEnchantPower(worldPointer, posX + dx * 2, posY, posZ + dz * 2);
                            power += ForgeHooks.getEnchantPower(worldPointer, posX + dx * 2, posY + 1, posZ + dz * 2);
                            if (dx != 0 && dz != 0) {
                                power += ForgeHooks.getEnchantPower(worldPointer, posX + dx * 2, posY, posZ + dz);
                                power += ForgeHooks.getEnchantPower(worldPointer, posX + dx * 2, posY + 1, posZ + dz);
                                power += ForgeHooks.getEnchantPower(worldPointer, posX + dx, posY, posZ + dz * 2);
                                power += ForgeHooks.getEnchantPower(worldPointer, posX + dx, posY + 1, posZ + dz * 2);
                            }
                        }
                    }
                }
                for (int i = 0; i < 3; ++i) enchantLevels[i] = EnchantmentHelper.calcItemStackEnchantability(rand, i, (int) power, stack);
                detectAndSendChanges();
            }
        } else {
            for (int i = 0; i < 3; ++i) enchantLevels[i] = 0;
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        ItemStack stack = tableInventory.getStackInSlot(0);
        if (id < 0 || id > 2 || enchantLevels[id] <= 0 || stack == null) return false;
        boolean creative = PlayerCapabilities.mcc$get$isCreativeMode(player.capabilities);
        if (EntityPlayer.mcc$get$experienceLevel(player) < enchantLevels[id] && !creative) return false;
        if (!worldPointer.isRemote) {
            List list = EnchantmentHelper.buildEnchantmentList(rand, stack, enchantLevels[id]);
            boolean book = stack.getItem() == Items.book;
            if (list != null) {
                player.addExperienceLevel(-enchantLevels[id]);
                if (book) stack.setItem(Items.enchanted_book);
                int skipped = book && list.size() > 1 ? rand.nextInt(list.size()) : -1;
                for (int i = 0; i < list.size(); ++i) {
                    EnchantmentData data = (EnchantmentData) list.get(i);
                    if (book && i == skipped) continue;
                    if (book) Items.enchanted_book.addEnchantment(stack, data);
                    else stack.addEnchantment(data.enchantmentobj, data.enchantmentLevel);
                }
                onCraftMatrixChanged(tableInventory);
            }
        }
        return true;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (worldPointer != null && !worldPointer.isRemote) {
            ItemStack stack = tableInventory.getStackInSlotOnClosing(0);
            if (stack != null) player.dropPlayerItemWithRandomChoice(stack, false);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return worldPointer.getBlock(posX, posY, posZ) == Blocks.enchanting_table
                && player.getDistanceSq(posX + 0.5D, posY + 0.5D, posZ + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack copy = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            copy = stack.copy();
            if (index == 0) {
                if (!mergeItemStack(stack, 1, 37, true)) return null;
            } else {
                Slot table = (Slot) inventorySlots.get(0);
                if (table.getHasStack() || !table.isItemValid(stack)) return null;
                if (stack.hasTagCompound() && stack.mcc$count() == 1) {
                    table.putStack(stack.copy());
                    stack.mcc$setCount(0);
                } else if (stack.mcc$count() >= 1) {
                    table.putStack(new ItemStack(stack.getItem(), 1, stack.getItemDamage()));
                    stack.mcc$setCount(stack.mcc$count() - 1);
                }
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.mcc$count() == copy.mcc$count()) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return copy;
    }
}
