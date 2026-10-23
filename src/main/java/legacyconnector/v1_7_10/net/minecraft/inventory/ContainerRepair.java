package legacyconnector.v1_7_10.net.minecraft.inventory;

import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The 1.7.10 anvil container, ported from vanilla: two input slots, one output slot and the repair/combine/rename
 * cost rules. Mods subclass it (EnderIO's Dark Steel Anvil) and read its private fields by name through reflection,
 * so the field names are the MCP ones.
 */
public class ContainerRepair extends Container {
    private IInventory outputSlot = new InventoryCraftResult();
    private IInventory inputSlots = new InventoryBasic("Repair", true, 2) {
        @Override
        public void markDirty() {
            super.markDirty();
            ContainerRepair.this.onCraftMatrixChanged(this);
        }
    };
    private World theWorld;
    private int field_82861_i;
    private int field_82858_j;
    private int field_82859_k;
    public int maximumCost;
    private int stackSizeToBeUsedInRepair;
    private String repairedItemName;
    private final EntityPlayer thePlayer;

    public ContainerRepair(InventoryPlayer inventory, final World world, final int x, final int y, final int z, EntityPlayer player) {
        this.theWorld = world;
        this.field_82861_i = x;
        this.field_82858_j = y;
        this.field_82859_k = z;
        this.thePlayer = player;
        addSlotToContainer(new Slot(inputSlots, 0, 27, 47));
        addSlotToContainer(new Slot(inputSlots, 1, 76, 47));
        addSlotToContainer(new Slot(outputSlot, 2, 134, 47) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeStack(EntityPlayer p) {
                return (creative(p) || EntityPlayer.mcc$get$experienceLevel(p) >= maximumCost) && maximumCost > 0 && getHasStack();
            }

            @Override
            public void onPickupFromSlot(EntityPlayer p, ItemStack stack) {
                if (!creative(p)) p.addExperienceLevel(-maximumCost);
                inputSlots.setInventorySlotContents(0, null);
                if (stackSizeToBeUsedInRepair > 0) {
                    ItemStack material = inputSlots.getStackInSlot(1);
                    if (material != null && material.mcc$count() > stackSizeToBeUsedInRepair) {
                        material.mcc$setCount(material.mcc$count() - stackSizeToBeUsedInRepair);
                        inputSlots.setInventorySlotContents(1, material);
                    } else {
                        inputSlots.setInventorySlotContents(1, null);
                    }
                } else {
                    inputSlots.setInventorySlotContents(1, null);
                }
                maximumCost = 0;
                if (!world.isRemote) world.playAuxSFX(1021, x, y, z, 0);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 142));
    }

    private static boolean creative(EntityPlayer p) {
        return legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$get$isCreativeMode(p.capabilities);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inv) {
        super.onCraftMatrixChanged(inv);
        if (inv == inputSlots) updateRepairOutput();
    }

    public void updateRepairOutput() {
        ItemStack input = inputSlots.getStackInSlot(0);
        maximumCost = 1;
        int cost = 0;
        int renameCost = 0;
        if (input == null) {
            outputSlot.setInventorySlotContents(0, null);
            maximumCost = 0;
            return;
        }
        ItemStack result = input.copy();
        ItemStack material = inputSlots.getStackInSlot(1);
        Map map = EnchantmentHelper.getEnchantments(result);
        boolean book = false;
        int baseCost = input.getRepairCost() + (material == null ? 0 : material.getRepairCost());
        stackSizeToBeUsedInRepair = 0;
        if (material != null) {
            book = material.getItem() == Items.enchanted_book && storedEnchantments(material) > 0;
            if (result.isItemStackDamageable() && result.getItem().getIsRepairable(input, material)) {
                int repair = Math.min(result.getItemDamageForDisplay(), result.getMaxDamage() / 4);
                if (repair <= 0) {
                    outputSlot.setInventorySlotContents(0, null);
                    maximumCost = 0;
                    return;
                }
                int used;
                for (used = 0; repair > 0 && used < material.mcc$count(); used++) {
                    result.setItemDamage(result.getItemDamageForDisplay() - repair);
                    cost += Math.max(1, repair / 100) + map.size();
                    repair = Math.min(result.getItemDamageForDisplay(), result.getMaxDamage() / 4);
                }
                stackSizeToBeUsedInRepair = used;
            } else {
                if (!book && (result.getItem() != material.getItem() || !result.isItemStackDamageable())) {
                    outputSlot.setInventorySlotContents(0, null);
                    maximumCost = 0;
                    return;
                }
                if (result.isItemStackDamageable() && !book) {
                    int left = input.getMaxDamage() - input.getItemDamageForDisplay();
                    int materialLeft = material.getMaxDamage() - material.getItemDamageForDisplay();
                    int bonus = materialLeft + result.getMaxDamage() * 12 / 100;
                    int damage = result.getMaxDamage() - (left + bonus);
                    if (damage < 0) damage = 0;
                    if (damage < result.getItemDamage()) {
                        result.setItemDamage(damage);
                        cost += Math.max(1, bonus / 100);
                    }
                }
                Map added = EnchantmentHelper.getEnchantments(material);
                if (book) added = storedEnchantmentMap(material);
                for (Object o : added.keySet()) {
                    int id = (Integer) o;
                    Enchantment enchantment = id >= 0 && id < Enchantment.enchantmentsList.length ? Enchantment.enchantmentsList[id] : null;
                    if (enchantment == null) continue;
                    int have = map.containsKey(id) ? (Integer) map.get(id) : 0;
                    int level = (Integer) added.get(id);
                    level = have == level ? level + 1 : Math.max(level, have);
                    boolean applies = enchantment.canApply(input);
                    if (creative(thePlayer) || input.getItem() == Items.enchanted_book) applies = true;
                    for (Object other : map.keySet()) {
                        int otherId = (Integer) other;
                        if (otherId != id && !enchantment.canApplyTogether(Enchantment.enchantmentsList[otherId])) {
                            applies = false;
                            cost += renameCost;
                        }
                    }
                    if (applies) {
                        if (level > enchantment.getMaxLevel()) level = enchantment.getMaxLevel();
                        map.put(id, level);
                        int weight = switch (enchantment.getWeight()) {
                            case 1 -> 8;
                            case 2 -> 4;
                            case 5 -> 2;
                            case 10 -> 1;
                            default -> 0;
                        };
                        cost += weight * level;
                    }
                }
            }
        }
        if (repairedItemName == null || repairedItemName.trim().isEmpty()) {
            if (input.hasDisplayName()) {
                renameCost = 1;
                cost += renameCost;
                result.func_135074_t();
            }
        } else if (!repairedItemName.equals(input.getDisplayName())) {
            renameCost = 1;
            cost += renameCost;
            result.setStackDisplayName(repairedItemName);
        }
        maximumCost = baseCost + cost;
        if (cost <= 0) result = null;
        if (renameCost == cost && renameCost > 0 && maximumCost >= 40) maximumCost = 39;
        if (maximumCost >= 40 && !creative(thePlayer)) result = null;
        if (result != null) {
            int repairCost = result.getRepairCost();
            if (material != null && repairCost < material.getRepairCost()) repairCost = material.getRepairCost();
            if (result.hasDisplayName()) repairCost -= 9;
            if (repairCost < 0) repairCost = 0;
            result.setRepairCost(repairCost + 2);
            EnchantmentHelper.setEnchantments(map, result);
        }
        outputSlot.setInventorySlotContents(0, result);
        detectAndSendChanges();
    }

    private static legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList stored(ItemStack book) {
        return book.getTagCompound() == null ? null : book.getTagCompound().getTagList("StoredEnchantments", 10);
    }

    private static int storedEnchantments(ItemStack book) {
        var list = stored(book);
        return list == null ? 0 : list.tagCount();
    }

    private static Map storedEnchantmentMap(ItemStack book) {
        Map<Integer, Integer> out = new java.util.LinkedHashMap<>();
        var list = stored(book);
        if (list != null) {
            for (int i = 0; i < list.tagCount(); i++) {
                var e = list.getCompoundTagAt(i);
                out.put((int) e.getShort("id"), (int) e.getShort("lvl"));
            }
        }
        return out;
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        crafter.sendProgressBarUpdate(this, 0, maximumCost);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Object c : crafters) ((ICrafting) c).sendProgressBarUpdate(this, 0, maximumCost);
    }

    @Override
    public void updateProgressBar(int id, int value) {
        if (id == 0) maximumCost = value;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!theWorld.isRemote) {
            for (int i = 0; i < inputSlots.getSizeInventory(); i++) {
                ItemStack stack = inputSlots.getStackInSlotOnClosing(i);
                if (stack != null) player.dropPlayerItemWithRandomChoice(stack, false);
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return theWorld.getBlock(field_82861_i, field_82858_j, field_82859_k) == Blocks.anvil
                && player.getDistanceSq(field_82861_i + 0.5D, field_82858_j + 0.5D, field_82859_k + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack copy = null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            copy = stack.copy();
            if (index == 2) {
                if (!mergeItemStack(stack, 3, 39, true)) return null;
                slot.onSlotChange(stack, copy);
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 39 && !mergeItemStack(stack, 0, 2, false)) return null;
            } else if (!mergeItemStack(stack, 3, 39, false)) {
                return null;
            }
            if (stack.mcc$count() == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.mcc$count() == copy.mcc$count()) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return copy;
    }

    public void updateItemName(String name) {
        this.repairedItemName = name;
        if (getSlot(2).getHasStack()) {
            ItemStack out = getSlot(2).getStack();
            if (name == null || name.trim().isEmpty()) out.func_135074_t();
            else out.setStackDisplayName(name);
        }
        updateRepairOutput();
    }
}
