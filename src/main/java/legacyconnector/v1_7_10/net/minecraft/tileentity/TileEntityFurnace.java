package legacyconnector.v1_7_10.net.minecraft.tileentity;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Legacy view of a furnace: the burn time lookups mods use for their own fuels and machines
 * ({@link #getItemBurnTime}, {@link #isItemFuel}) answer from the modern fuel table plus the NeoForge burn time event
 * (which legacy {@code IFuelHandler}s are registered on); the instance part is a plain three-slot inventory.
 */
public class TileEntityFurnace extends TileEntity implements IInventory {
    private ItemStack[] furnaceItemStacks = new ItemStack[3];
    public int furnaceBurnTime;
    public int currentItemBurnTime;
    public int furnaceCookTime;
    private String field_145958_o;

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

    public void func_145951_a(String name) {
        this.field_145958_o = name;
    }

    @Override
    public int getSizeInventory() {
        return furnaceItemStacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return furnaceItemStacks[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
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
        ItemStack stack = furnaceItemStacks[slot];
        furnaceItemStacks[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        furnaceItemStacks[slot] = stack;
        if (stack != null && ItemStack.mcc$get$stackSize(stack) > getInventoryStackLimit()) {
            ItemStack.mcc$set$stackSize(stack, getInventoryStackLimit());
        }
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
        return true;
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
}
