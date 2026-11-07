package legacyconnector.v1_6_4.net.minecraftforge.liquids;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/**
 * Forge 1.5.2 and 1.6.4's liquid API ({@code net.minecraftforge.liquids}, replaced by fluids in 1.7): a liquid is the block or item with an id
 * that stands for it, an amount in millibuckets and a metadata value. This is the data class with the behaviour mods relied on
 * (equality, NBT, the stack of the liquid block), kept on the numeric-id facade so a liquid keeps meaning the same block as in the old game.
 */
public class LiquidStack {
    public int itemID;
    public int amount;
    public int itemMeta;

    public LiquidStack() {
    }

    public LiquidStack(int itemID, int amount) {
        this(itemID, amount, 0);
    }

    public LiquidStack(int itemID, int amount, int itemDamage) {
        this.itemID = itemID;
        this.amount = amount;
        this.itemMeta = itemDamage;
    }

    public LiquidStack(Item item, int amount) {
        this(item, amount, 0);
    }

    public LiquidStack(Item item, int amount, int itemDamage) {
        this(Ids.itemId(item), amount, itemDamage);
    }

    public LiquidStack(Block block, int amount) {
        this(Ids.blockId(block), amount, 0);
    }

    public LiquidStack(Block block, int amount, int itemDamage) {
        this(Ids.blockId(block), amount, itemDamage);
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setShort("Id", (short) itemID);
        nbt.setInteger("Amount", amount);
        nbt.setShort("Meta", (short) itemMeta);
        String name = LiquidDictionary.findLiquidName(this);
        if (name != null) nbt.setString("LiquidName", name);
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        String name = nbt.hasKey("LiquidName") ? nbt.getString("LiquidName") : null;
        if (name != null) {
            LiquidStack canonical = LiquidDictionary.getCanonicalLiquid(name);
            if (canonical != null) {
                itemID = canonical.itemID;
                itemMeta = canonical.itemMeta;
                amount = nbt.getInteger("Amount");
                return;
            }
        }
        itemID = nbt.getShort("Id");
        amount = nbt.getInteger("Amount");
        itemMeta = nbt.getShort("Meta");
    }

    public static LiquidStack loadLiquidStackFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;
        LiquidStack stack = new LiquidStack();
        stack.readFromNBT(nbt);
        return stack.itemID == 0 ? null : stack;
    }

    public LiquidStack copy() {
        return new LiquidStack(itemID, amount, itemMeta);
    }

    /** Same liquid (id and metadata), whatever the amounts. */
    public boolean isLiquidEqual(LiquidStack other) {
        return other != null && itemID == other.itemID && itemMeta == other.itemMeta;
    }

    public boolean containsLiquid(LiquidStack other) {
        return isLiquidEqual(other) && amount >= other.amount;
    }

    /** The block or item a liquid is made of, as a stack of one. */
    public ItemStack asItemStack() {
        Item item = Ids.item(itemID);
        return item == null ? null : new ItemStack(item, 1, itemMeta);
    }

    public static LiquidStack fromItemStack(ItemStack stack) {
        return stack == null || stack.getItem() == null ? null : new LiquidStack(Ids.itemId(stack.getItem()), 1, stack.getItemDamage());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LiquidStack s && s.itemID == itemID && s.itemMeta == itemMeta && s.amount == amount;
    }

    @Override
    public int hashCode() {
        return itemID * 31 * 31 + itemMeta * 31 + amount;
    }

    @Override
    public String toString() {
        return "LiquidStack{" + itemID + ":" + itemMeta + " x" + amount + "}";
    }
}
