package legacyconnector.v1_12_2.net.minecraft.inventory;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentData;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemEnchantedBook;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 enchanting table container: the item and lapis slots, the three offered levels ({@link #enchantLevels}), the first enchantment
 * each button hints at ({@link #enchantClue}/{@link #worldClue}) and the player's seed. The rules are the 1.12 ones, run on the legacy
 * {@code EnchantmentHelper} (so modded enchantments and {@code getEnchantPowerBonus} of modded shelves count): the bookshelf ring is read
 * through the world, the roll through the shared seeded random, the cost is levels plus lapis.
 */
public class ContainerEnchantment extends Container {
    public IInventory tableInventory = new InventoryBasic("Enchant", true, 2) {
        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {
            super.markDirty();
            ContainerEnchantment.this.onCraftMatrixChanged(this);
        }
    };
    private final World world;
    private final BlockPos position;
    private final Random rand = new Random();
    public int xpSeed;
    public int[] enchantLevels = new int[3];
    public int[] enchantClue = new int[] {-1, -1, -1};
    public int[] worldClue = new int[] {-1, -1, -1};

    public ContainerEnchantment(InventoryPlayer playerInv, World worldIn) {
        this(playerInv, worldIn, BlockPos.ORIGIN);
    }

    public ContainerEnchantment(InventoryPlayer playerInv, World worldIn, BlockPos pos) {
        this.world = worldIn;
        this.position = pos;
        this.xpSeed = playerInv.player.getXPSeed();
        addSlotToContainer(new Slot(tableInventory, 0, 15, 47) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return true;
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });
        addSlotToContainer(new Slot(tableInventory, 1, 35, 47) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == Items.DYE && EnumDyeColor.byDyeDamage(stack.getMetadata()) == EnumDyeColor.BLUE;
            }
        });
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        }
        for (int k = 0; k < 9; ++k) addSlotToContainer(new Slot(playerInv, k, 8 + k * 18, 142));
    }

    private void sendProperties(IContainerListener listener) {
        listener.sendWindowProperty(this, 0, enchantLevels[0]);
        listener.sendWindowProperty(this, 1, enchantLevels[1]);
        listener.sendWindowProperty(this, 2, enchantLevels[2]);
        listener.sendWindowProperty(this, 3, xpSeed & -16);
        listener.sendWindowProperty(this, 4, enchantClue[0]);
        listener.sendWindowProperty(this, 5, enchantClue[1]);
        listener.sendWindowProperty(this, 6, enchantClue[2]);
        listener.sendWindowProperty(this, 7, worldClue[0]);
        listener.sendWindowProperty(this, 8, worldClue[1]);
        listener.sendWindowProperty(this, 9, worldClue[2]);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (IContainerListener listener : listeners) sendProperties(listener);
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id >= 0 && id <= 2) enchantLevels[id] = data;
        else if (id == 3) xpSeed = data;
        else if (id >= 4 && id <= 6) enchantClue[id - 4] = data;
        else if (id >= 7 && id <= 9) worldClue[id - 7] = data;
        else super.updateProgressBar(id, data);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        if (inventoryIn != tableInventory) return;
        ItemStack itemstack = inventoryIn.getStackInSlot(0);
        if (!itemstack.isEmpty() && itemstack.isItemEnchantable()) {
            if (!world.isRemote) {
                int power = 0;
                for (int j = -1; j <= 1; ++j) {
                    for (int k = -1; k <= 1; ++k) {
                        if ((j != 0 || k != 0) && world.isAirBlock(position.add(k, 0, j)) && world.isAirBlock(position.add(k, 1, j))) {
                            power += getEnchantPower(position.add(k * 2, 0, j * 2));
                            power += getEnchantPower(position.add(k * 2, 1, j * 2));
                            if (k != 0 && j != 0) {
                                power += getEnchantPower(position.add(k * 2, 0, j));
                                power += getEnchantPower(position.add(k * 2, 1, j));
                                power += getEnchantPower(position.add(k, 0, j * 2));
                                power += getEnchantPower(position.add(k, 1, j * 2));
                            }
                        }
                    }
                }
                rand.setSeed(xpSeed);
                for (int i = 0; i < 3; ++i) {
                    enchantLevels[i] = EnchantmentHelper.calcItemStackEnchantability(rand, i, power, itemstack);
                    enchantClue[i] = -1;
                    worldClue[i] = -1;
                    if (enchantLevels[i] < i + 1) enchantLevels[i] = 0;
                }
                for (int i = 0; i < 3; ++i) {
                    if (enchantLevels[i] > 0) {
                        List<EnchantmentData> list = getEnchantmentList(itemstack, i, enchantLevels[i]);
                        if (list != null && !list.isEmpty()) {
                            EnchantmentData data = list.get(rand.nextInt(list.size()));
                            enchantClue[i] = Enchantment.getEnchantmentID(data.enchantment);
                            worldClue[i] = data.enchantmentLevel;
                        }
                    }
                }
                detectAndSendChanges();
            }
        } else {
            for (int i = 0; i < 3; ++i) {
                enchantLevels[i] = 0;
                enchantClue[i] = -1;
                worldClue[i] = -1;
            }
        }
    }

    private float getEnchantPower(BlockPos pos) {
        return world.getBlockState(pos).getBlock().getEnchantPowerBonus(world, pos);
    }

    @Override
    public boolean enchantItem(EntityPlayer playerIn, int id) {
        if (id < 0 || id > 2) return false;
        ItemStack itemstack = tableInventory.getStackInSlot(0);
        ItemStack lapis = tableInventory.getStackInSlot(1);
        int cost = id + 1;
        var p = playerIn.mcc$player();
        boolean creative = p != null && p.getAbilities().instabuild;
        int xpLevel = p == null ? 0 : p.experienceLevel;
        if ((lapis.isEmpty() || lapis.getCount() < cost) && !creative) return false;
        if (enchantLevels[id] > 0 && !itemstack.isEmpty() && (xpLevel >= cost && xpLevel >= enchantLevels[id] || creative)) {
            if (!world.isRemote) {
                List<EnchantmentData> list = getEnchantmentList(itemstack, id, enchantLevels[id]);
                if (!list.isEmpty()) {
                    playerIn.onEnchant(itemstack, cost);
                    boolean book = itemstack.getItem() == Items.BOOK;
                    if (book) {
                        itemstack = new ItemStack(Items.ENCHANTED_BOOK);
                        tableInventory.setInventorySlotContents(0, itemstack);
                    }
                    for (EnchantmentData data : list) {
                        if (book) ItemEnchantedBook.addEnchantment(itemstack, data);
                        else itemstack.addEnchantment(data.enchantment, data.enchantmentLevel);
                    }
                    if (!creative) {
                        lapis.shrink(cost);
                        if (lapis.isEmpty()) tableInventory.setInventorySlotContents(1, ItemStack.EMPTY);
                    }
                    tableInventory.markDirty();
                    xpSeed = playerIn.getXPSeed();
                    onCraftMatrixChanged(tableInventory);
                    world.playSound(null, position, legacyconnector.v1_12_2.net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                            legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.BLOCKS, 1.0F, rand.nextFloat() * 0.1F + 0.9F);
                }
            }
            return true;
        }
        return false;
    }

    private List<EnchantmentData> getEnchantmentList(ItemStack stack, int enchantSlot, int level) {
        rand.setSeed((long) (xpSeed + enchantSlot));
        List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(rand, stack, level, false);
        if (stack.getItem() == Items.BOOK && list.size() > 1) list.remove(rand.nextInt(list.size()));
        return list;
    }

    public int getLapisAmount() {
        ItemStack stack = tableInventory.getStackInSlot(1);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        if (!world.isRemote) clearContainer(playerIn, world, tableInventory);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        if (position != BlockPos.ORIGIN && world.getBlockState(position).getBlock() != Blocks.ENCHANTING_TABLE) return false;
        return position == BlockPos.ORIGIN
                || playerIn.getDistanceSq((double) position.getX() + 0.5D, (double) position.getY() + 0.5D, (double) position.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index == 0) {
                if (!mergeItemStack(itemstack1, 2, 38, true)) return ItemStack.EMPTY;
            } else if (index == 1) {
                if (!mergeItemStack(itemstack1, 2, 38, true)) return ItemStack.EMPTY;
            } else if (itemstack1.getItem() == Items.DYE && EnumDyeColor.byDyeDamage(itemstack1.getMetadata()) == EnumDyeColor.BLUE) {
                if (!mergeItemStack(itemstack1, 1, 2, true)) return ItemStack.EMPTY;
            } else {
                if (inventorySlots.get(0).getHasStack() || !inventorySlots.get(0).isItemValid(itemstack1)) return ItemStack.EMPTY;
                ItemStack single = itemstack1.copy();
                single.setCount(1);
                itemstack1.shrink(1);
                inventorySlots.get(0).putStack(single);
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }
}
