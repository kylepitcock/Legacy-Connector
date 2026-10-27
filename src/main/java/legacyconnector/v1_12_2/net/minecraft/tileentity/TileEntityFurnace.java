package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.inventory.ISidedInventory;
import legacyconnector.v1_12_2.net.minecraft.inventory.ItemStackHelper;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.FurnaceRecipes;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.ITickable;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.items.CapabilityItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.IItemHandler;
import legacyconnector.v1_12_2.net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2's furnace: input, fuel and output slots, burn and cook times, smelting through {@link FurnaceRecipes}. Fuel values are the
 * modern game's (vanilla fuels, Forge's burn time of mod items). Mods' furnaces extend it; a furnace, smoker or blast furnace of the
 * game is seen through a view over the modern block entity (its slots and its four progress values).
 */
public class TileEntityFurnace extends TileEntityLockable implements ITickable, ISidedInventory {
    private static final int[] SLOTS_TOP = new int[] {0};
    private static final int[] SLOTS_BOTTOM = new int[] {2, 1};
    private static final int[] SLOTS_SIDES = new int[] {1};
    private NonNullList<ItemStack> furnaceItemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private int furnaceBurnTime;
    private int currentItemBurnTime;
    private int cookTime;
    private int totalCookTime;
    private String furnaceCustomName;
    private final net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity mcc$furnace;
    private net.minecraft.world.inventory.ContainerData mcc$data;
    IItemHandler handlerTop = new SidedInvWrapper(this, EnumFacing.UP);
    IItemHandler handlerBottom = new SidedInvWrapper(this, EnumFacing.DOWN);
    IItemHandler handlerSide = new SidedInvWrapper(this, EnumFacing.WEST);

    public TileEntityFurnace() {
        this.mcc$furnace = null;
    }

    /** A view of a game furnace. */
    public TileEntityFurnace(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
        this.mcc$furnace = furnace;
        this.furnaceItemStacks = new org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernStackList1122(furnace);
        try {
            var f = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.class.getDeclaredField("dataAccess");
            f.setAccessible(true);
            mcc$data = (net.minecraft.world.inventory.ContainerData) f.get(furnace);
        } catch (ReflectiveOperationException | RuntimeException e) {
            mcc$data = null;
        }
        mcc$refresh();
    }

    /** Copies a view's progress values from the modern furnace. */
    public void mcc$refresh() {
        if (mcc$data == null) return;
        furnaceBurnTime = mcc$data.get(0);
        currentItemBurnTime = mcc$data.get(1);
        cookTime = mcc$data.get(2);
        totalCookTime = mcc$data.get(3);
    }

    @Override
    public int getSizeInventory() {
        return furnaceItemStacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : furnaceItemStacks) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return furnaceItemStacks.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(furnaceItemStacks, index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(furnaceItemStacks, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (stack == null) stack = ItemStack.EMPTY;
        ItemStack before = furnaceItemStacks.get(index);
        boolean same = !stack.isEmpty() && stack.isItemEqual(before) && ItemStack.areItemStackTagsEqual(stack, before);
        furnaceItemStacks.set(index, stack);
        if (stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
        if (index == 0 && !same && mcc$furnace == null) {
            totalCookTime = getCookTime(stack);
            cookTime = 0;
            markDirty();
        }
    }

    @Override
    public String getName() {
        return hasCustomName() ? furnaceCustomName : "container.furnace";
    }

    @Override
    public boolean hasCustomName() {
        return furnaceCustomName != null && !furnaceCustomName.isEmpty();
    }

    public void setCustomInventoryName(String name) {
        this.furnaceCustomName = name;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (mcc$furnace == null) furnaceItemStacks = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, furnaceItemStacks);
        furnaceBurnTime = compound.getInteger("BurnTime");
        cookTime = compound.getInteger("CookTime");
        totalCookTime = compound.getInteger("CookTimeTotal");
        currentItemBurnTime = getItemBurnTime(furnaceItemStacks.get(1));
        if (compound.hasKey("CustomName", 8)) furnaceCustomName = compound.getString("CustomName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setShort("BurnTime", (short) furnaceBurnTime);
        compound.setShort("CookTime", (short) cookTime);
        compound.setShort("CookTimeTotal", (short) totalCookTime);
        ItemStackHelper.saveAllItems(compound, furnaceItemStacks);
        if (hasCustomName()) compound.setString("CustomName", furnaceCustomName);
        return compound;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    public boolean isBurning() {
        if (mcc$data != null) return mcc$data.get(0) > 0;
        return furnaceBurnTime > 0;
    }

    /** Whether a furnace inventory burns (its field 0, the burn time left). */
    public static boolean isBurning(IInventory inventory) {
        return inventory.getField(0) > 0;
    }

    @Override
    public void update() {
        if (mcc$furnace != null) return; // the game ticks its own furnaces
        boolean wasBurning = isBurning();
        boolean dirty = false;
        if (isBurning()) --furnaceBurnTime;
        if (world != null && !world.isRemote) {
            ItemStack fuel = furnaceItemStacks.get(1);
            if (isBurning() || !fuel.isEmpty() && !furnaceItemStacks.get(0).isEmpty()) {
                if (!isBurning() && canSmelt()) {
                    furnaceBurnTime = getItemBurnTime(fuel);
                    currentItemBurnTime = furnaceBurnTime;
                    if (isBurning()) {
                        dirty = true;
                        if (!fuel.isEmpty()) {
                            Item item = fuel.getItem();
                            fuel.shrink(1);
                            if (fuel.isEmpty()) furnaceItemStacks.set(1, item.getContainerItem(fuel));
                        }
                    }
                }
                if (isBurning() && canSmelt()) {
                    ++cookTime;
                    if (cookTime == totalCookTime) {
                        cookTime = 0;
                        totalCookTime = getCookTime(furnaceItemStacks.get(0));
                        smeltItem();
                        dirty = true;
                    }
                } else {
                    cookTime = 0;
                }
            } else if (!isBurning() && cookTime > 0) {
                cookTime = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.clamp(cookTime - 2, 0, totalCookTime);
            }
            if (wasBurning != isBurning()) dirty = true;
        }
        if (dirty) markDirty();
    }

    public int getCookTime(ItemStack stack) {
        return 200;
    }

    private boolean canSmelt() {
        if (furnaceItemStacks.get(0).isEmpty()) return false;
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(furnaceItemStacks.get(0));
        if (result.isEmpty()) return false;
        ItemStack out = furnaceItemStacks.get(2);
        if (out.isEmpty()) return true;
        if (!out.isItemEqual(result)) return false;
        int total = out.getCount() + result.getCount();
        if (total <= getInventoryStackLimit() && total <= out.getMaxStackSize()) return true;
        return total <= result.getMaxStackSize();
    }

    /** Turns one input item into its smelting result. */
    public void smeltItem() {
        if (!canSmelt()) return;
        ItemStack input = furnaceItemStacks.get(0);
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input);
        ItemStack out = furnaceItemStacks.get(2);
        if (out.isEmpty()) furnaceItemStacks.set(2, result.copy());
        else if (out.getItem() == result.getItem()) out.grow(result.getCount());
        input.shrink(1);
    }

    /** The ticks a stack burns as furnace fuel (0 when it is no fuel), as the game rates it: vanilla fuels, mods' burn times. */
    public static int getItemBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var modern = Stacks1122.toModern(stack);
        if (modern.isEmpty()) return 0;
        return modern.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
    }

    public static boolean isItemFuel(ItemStack stack) {
        return getItemBurnTime(stack) > 0;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null || world.getTileEntity(pos) != this) return false;
        return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == 2) return false;
        if (index != 1) return true;
        Item item = stack.getItem();
        return isItemFuel(stack) || item == legacyconnector.v1_12_2.net.minecraft.init.Items.BUCKET
                && furnaceItemStacks.get(1).getItem() != legacyconnector.v1_12_2.net.minecraft.init.Items.BUCKET;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        if (side == EnumFacing.DOWN) return SLOTS_BOTTOM;
        return side == EnumFacing.UP ? SLOTS_TOP : SLOTS_SIDES;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return isItemValidForSlot(index, itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        if (direction == EnumFacing.DOWN && index == 1) {
            Item item = stack.getItem();
            return item == legacyconnector.v1_12_2.net.minecraft.init.Items.WATER_BUCKET || item == legacyconnector.v1_12_2.net.minecraft.init.Items.BUCKET;
        }
        return true;
    }

    @Override
    public String getGuiID() {
        return "minecraft:furnace";
    }

    /** 1.12's ContainerFurnace is not part of this layer yet: furnace GUIs are the game's (mods open their own containers). */
    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return null;
    }

    @Override
    public int getField(int id) {
        if (mcc$data != null) return id >= 0 && id < 4 ? mcc$data.get(id) : 0;
        return switch (id) {
            case 0 -> furnaceBurnTime;
            case 1 -> currentItemBurnTime;
            case 2 -> cookTime;
            case 3 -> totalCookTime;
            default -> 0;
        };
    }

    @Override
    public void setField(int id, int value) {
        if (mcc$data != null && id >= 0 && id < 4) mcc$data.set(id, value);
        switch (id) {
            case 0 -> furnaceBurnTime = value;
            case 1 -> currentItemBurnTime = value;
            case 2 -> cookTime = value;
            case 3 -> totalCookTime = value;
            default -> { }
        }
    }

    @Override
    public int getFieldCount() {
        return 4;
    }

    @Override
    public void clear() {
        furnaceItemStacks.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (facing != null && capability != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.DOWN) return (T) handlerBottom;
            if (facing == EnumFacing.UP) return (T) handlerTop;
            return (T) handlerSide;
        }
        return super.getCapability(capability, facing);
    }
}
