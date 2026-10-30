package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import net.minecraft.world.item.crafting.RecipeType;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/**
 * The 1.7.10 furnace, ported: input slot 0, fuel slot 1, output slot 2; it burns fuel and smelts an item every 200 ticks (mods
 * subclass it for their own furnaces). The burn time lookups mods use for their own fuels and machines ({@link #getItemBurnTime},
 * {@link #isItemFuel}) answer from the modern fuel table plus the NeoForge burn time event (which legacy {@code IFuelHandler}s are
 * registered on). For a vanilla furnace (smoker, blast furnace) it is a view: the slots are the modern furnace's, the burn and cook
 * times are read when the view is made and written back on {@link #markDirty()} (Immersive Engineering's external heater keeps
 * vanilla furnaces burning this way).
 */
public class TileEntityFurnace extends TileEntity implements ISidedInventory {
    private static final int[] slotsTop = {0};
    private static final int[] slotsBottom = {2, 1};
    private static final int[] slotsSides = {1};
    private static final Class<?> MCC$MODERN = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class;
    private ItemStack[] furnaceItemStacks = new ItemStack[3];
    public int furnaceBurnTime;
    public int currentItemBurnTime;
    public int furnaceCookTime;
    private String field_145958_o;
    /** The modern furnace this views; null for a legacy furnace. */
    private final net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity mcc$furnace;

    public TileEntityFurnace() {
        this.mcc$furnace = null;
    }

    /** View of a vanilla furnace. */
    public TileEntityFurnace(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
        this.mcc$furnace = furnace;
        mcc$refresh();
    }

    /** The times last read from the modern furnace: only fields a mod changed since are written back. */
    private int mcc$seenBurn;
    private int mcc$seenItemBurn;
    private int mcc$seenCook;

    /** Reads the modern furnace's burn and cook times. */
    public void mcc$refresh() {
        if (mcc$furnace == null) return;
        furnaceBurnTime = mcc$seenBurn = VanillaOps.getInt(mcc$furnace, MCC$MODERN, "litTime", furnaceBurnTime);
        currentItemBurnTime = mcc$seenItemBurn = VanillaOps.getInt(mcc$furnace, MCC$MODERN, "litDuration", currentItemBurnTime);
        furnaceCookTime = mcc$seenCook = VanillaOps.getInt(mcc$furnace, MCC$MODERN, "cookingProgress", furnaceCookTime);
    }

    /**
     * Writes the burn and cook times a mod changed back into the modern furnace (lighting it when it now burns). A view a mod
     * kept for a while holds old times, so unchanged fields are left alone.
     */
    public void mcc$flush() {
        if (mcc$furnace == null) return;
        boolean any = false;
        if (furnaceBurnTime != mcc$seenBurn) {
            VanillaOps.setInt(mcc$furnace, MCC$MODERN, "litTime", mcc$seenBurn = furnaceBurnTime);
            any = true;
        }
        if (currentItemBurnTime != mcc$seenItemBurn) {
            VanillaOps.setInt(mcc$furnace, MCC$MODERN, "litDuration", mcc$seenItemBurn = currentItemBurnTime);
            any = true;
        }
        if (furnaceCookTime != mcc$seenCook) {
            VanillaOps.setInt(mcc$furnace, MCC$MODERN, "cookingProgress", mcc$seenCook = furnaceCookTime);
            any = true;
        }
        if (!any) return;
        var level = mcc$furnace.getLevel();
        if (level != null) {
            var pos = mcc$furnace.getBlockPos();
            var state = level.getBlockState(pos);
            var lit = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
            if (state.hasProperty(lit) && state.getValue(lit) != furnaceBurnTime > 0) level.setBlock(pos, state.setValue(lit, furnaceBurnTime > 0), 3);
        }
        mcc$furnace.setChanged();
    }

    /** Burn time in ticks, 0 when the stack is not a fuel. */
    public static int getItemBurnTime(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        net.minecraft.world.item.ItemStack modern = ItemStack.mcc$toModern(stack);
        if (modern == null || modern.isEmpty()) return 0;
        // NeoForge: -1 means "use the vanilla fuel table"; the event lets handlers (legacy IFuelHandlers) override.
        int time = modern.getBurnTime(RecipeType.SMELTING);
        if (time < 0) {
            time = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().getOrDefault(modern.getItem(), 0);
        }
        // The modern table is a data map, which is empty while mods load; mods read burn times then (to scale their own
        // recipes), so fall back to the 1.7.10 table.
        if (time <= 0) time = vanillaBurnTime(modern);
        return Math.max(0, net.neoforged.neoforge.event.EventHooks.getItemBurnTime(modern, time, RecipeType.SMELTING));
    }

    /** The fuel values of the 1.7.10 {@code TileEntityFurnace.getItemBurnTime}. */
    private static int vanillaBurnTime(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.Item item = stack.getItem();
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        switch (path) {
            case "coal", "charcoal": return 1600;
            case "coal_block": return 16000;
            case "blaze_rod": return 2400;
            case "lava_bucket": return 20000;
            case "stick": return 100;
            default: break;
        }
        if (path.endsWith("_sapling")) return 100;
        if (path.startsWith("wooden_") && (path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_axe")
                || path.endsWith("_shovel") || path.endsWith("_hoe"))) return 200;
        if (item instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock().defaultBlockState().getSoundType() == net.minecraft.world.level.block.SoundType.WOOD) {
            return path.endsWith("_slab") ? 150 : 300;
        }
        return 0;
    }

    public static boolean isItemFuel(ItemStack stack) {
        return getItemBurnTime(stack) > 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (mcc$furnace != null) return;
        NBTTagList list = tag.getTagList("Items", 10);
        furnaceItemStacks = new ItemStack[getSizeInventory()];
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound item = list.getCompoundTagAt(i);
            byte slot = item.getByte("Slot");
            if (slot >= 0 && slot < furnaceItemStacks.length) furnaceItemStacks[slot] = ItemStack.loadItemStackFromNBT(item);
        }
        furnaceBurnTime = tag.getShort("BurnTime");
        furnaceCookTime = tag.getShort("CookTime");
        currentItemBurnTime = getItemBurnTime(furnaceItemStacks[1]);
        if (tag.hasKey("CustomName", 8)) field_145958_o = tag.getString("CustomName");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setShort("BurnTime", (short) furnaceBurnTime);
        tag.setShort("CookTime", (short) furnaceCookTime);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack == null) continue;
            NBTTagCompound item = new NBTTagCompound();
            item.setByte("Slot", (byte) i);
            stack.writeToNBT(item);
            list.appendTag(item);
        }
        tag.setTag("Items", list);
        if (hasCustomInventoryName()) tag.setString("CustomName", field_145958_o);
    }

    public boolean isBurning() {
        return furnaceBurnTime > 0;
    }

    public int getBurnTimeRemainingScaled(int scale) {
        if (currentItemBurnTime == 0) currentItemBurnTime = 200;
        return furnaceBurnTime * scale / currentItemBurnTime;
    }

    public int getCookProgressScaled(int scale) {
        return furnaceCookTime * scale / 200;
    }

    /** Burns fuel and smelts (a vanilla furnace's view does nothing: the modern furnace runs itself). */
    @Override
    public void updateEntity() {
        if (mcc$furnace != null) return;
        boolean wasBurning = furnaceBurnTime > 0;
        boolean changed = false;
        if (furnaceBurnTime > 0) --furnaceBurnTime;
        if (worldObj != null && !worldObj.isRemote) {
            if (furnaceBurnTime != 0 || furnaceItemStacks[1] != null && furnaceItemStacks[0] != null) {
                if (furnaceBurnTime == 0 && canSmelt()) {
                    currentItemBurnTime = furnaceBurnTime = getItemBurnTime(furnaceItemStacks[1]);
                    if (furnaceBurnTime > 0) {
                        changed = true;
                        ItemStack fuel = furnaceItemStacks[1];
                        if (fuel != null) {
                            fuel.mcc$setCount(fuel.mcc$count() - 1);
                            if (fuel.mcc$count() == 0) furnaceItemStacks[1] = fuel.getItem().getContainerItem(fuel);
                        }
                    }
                }
                if (isBurning() && canSmelt()) {
                    ++furnaceCookTime;
                    if (furnaceCookTime == 200) {
                        furnaceCookTime = 0;
                        smeltItem();
                        changed = true;
                    }
                } else {
                    furnaceCookTime = 0;
                }
            }
            if (wasBurning != furnaceBurnTime > 0) {
                changed = true;
                legacyconnector.v1_7_10.net.minecraft.block.BlockFurnace.updateFurnaceBlockState(furnaceBurnTime > 0, worldObj, xCoord, yCoord, zCoord);
            }
        }
        if (changed) markDirty();
    }

    /** Whether the input has a smelting result that fits into the output slot. */
    private boolean canSmelt() {
        if (furnaceItemStacks[0] == null) return false;
        ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(furnaceItemStacks[0]);
        if (result == null) return false;
        if (furnaceItemStacks[2] == null) return true;
        if (!furnaceItemStacks[2].isItemEqual(result)) return false;
        int total = furnaceItemStacks[2].mcc$count() + result.mcc$count();
        return total <= getInventoryStackLimit() && total <= furnaceItemStacks[2].getMaxStackSize();
    }

    /** Turns one input item into its smelting result in the output slot. */
    public void smeltItem() {
        if (mcc$furnace != null || !canSmelt()) return;
        ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(furnaceItemStacks[0]);
        if (furnaceItemStacks[2] == null) furnaceItemStacks[2] = result.copy();
        else if (furnaceItemStacks[2].getItem() == result.getItem()) furnaceItemStacks[2].mcc$setCount(furnaceItemStacks[2].mcc$count() + result.mcc$count());
        furnaceItemStacks[0].mcc$setCount(furnaceItemStacks[0].mcc$count() - 1);
        if (furnaceItemStacks[0].mcc$count() <= 0) furnaceItemStacks[0] = null;
    }

    @Override
    public void markDirty() {
        mcc$flush();
        super.markDirty();
    }

    /** The SRG name of {@link #setCustomInventoryName} (1.7.10's mappings name it {@code setCustomInventoryName}); shim code still calls this one. */
    public void func_145951_a(String name) {
        setCustomInventoryName(name);
    }

    public void setCustomInventoryName(String name) {
        this.field_145958_o = name;
    }

    @Override
    public int getSizeInventory() {
        return mcc$furnace != null ? mcc$furnace.getContainerSize() : furnaceItemStacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (mcc$furnace != null) return slot >= 0 && slot < mcc$furnace.getContainerSize() ? ItemStack.mcc$wrap(mcc$furnace.getItem(slot)) : null;
        return slot >= 0 && slot < furnaceItemStacks.length ? furnaceItemStacks[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (mcc$furnace != null) return ItemStack.mcc$wrap(mcc$furnace.removeItem(slot, amount));
        ItemStack stack = furnaceItemStacks[slot];
        if (stack == null) return null;
        if (ItemStack.mcc$get$stackSize(stack) <= amount) {
            furnaceItemStacks[slot] = null;
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (ItemStack.mcc$get$stackSize(stack) == 0) furnaceItemStacks[slot] = null;
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (mcc$furnace != null) return ItemStack.mcc$wrap(mcc$furnace.removeItemNoUpdate(slot));
        ItemStack stack = furnaceItemStacks[slot];
        furnaceItemStacks[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (stack != null && ItemStack.mcc$get$stackSize(stack) > getInventoryStackLimit()) {
            ItemStack.mcc$set$stackSize(stack, getInventoryStackLimit());
        }
        if (mcc$furnace != null) {
            mcc$furnace.setItem(slot, ItemStack.mcc$toModern(stack));
            return;
        }
        furnaceItemStacks[slot] = stack;
    }

    @Override
    public String getInventoryName() {
        return hasCustomInventoryName() ? field_145958_o : "container.furnace";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return field_145958_o != null && !field_145958_o.isEmpty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null) return true;
        return worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != 2 && (slot != 1 || isItemFuel(stack));
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? slotsBottom : side == 1 ? slotsTop : slotsSides;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return side != 0 || slot != 1 || stack.getItem() == legacyconnector.v1_7_10.net.minecraft.init.Items.bucket;
    }
}
