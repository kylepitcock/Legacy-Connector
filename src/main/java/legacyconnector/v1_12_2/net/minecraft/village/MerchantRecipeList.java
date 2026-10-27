package legacyconnector.v1_12_2.net.minecraft.village;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;

/** 1.12.2 list of a merchant's trades (verbatim 1.12 matching, NBT and packet format). */
public class MerchantRecipeList extends java.util.ArrayList<MerchantRecipe> {
    public MerchantRecipeList() {
    }

    public MerchantRecipeList(NBTTagCompound tag) {
        readRecipiesFromTags(tag);
    }

    /** The trade at {@code index} (or the first that matches) that the two offered stacks pay for, or null. */
    public MerchantRecipe canRecipeBeUsed(ItemStack stack0, ItemStack stack1, int index) {
        if (index > 0 && index < size()) {
            MerchantRecipe r = get(index);
            return !areItemStacksExactlyEqual(stack0, r.getItemToBuy()) || (!stack1.isEmpty() || r.hasSecondItemToBuy())
                    && (!r.hasSecondItemToBuy() || !areItemStacksExactlyEqual(stack1, r.getSecondItemToBuy()))
                    || stack0.getCount() < r.getItemToBuy().getCount()
                    || r.hasSecondItemToBuy() && stack1.getCount() < r.getSecondItemToBuy().getCount() ? null : r;
        }
        for (MerchantRecipe r : this) {
            if (areItemStacksExactlyEqual(stack0, r.getItemToBuy()) && stack0.getCount() >= r.getItemToBuy().getCount()
                    && (!r.hasSecondItemToBuy() && stack1.isEmpty()
                    || r.hasSecondItemToBuy() && areItemStacksExactlyEqual(stack1, r.getSecondItemToBuy()) && stack1.getCount() >= r.getSecondItemToBuy().getCount())) {
                return r;
            }
        }
        return null;
    }

    private boolean areItemStacksExactlyEqual(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b) && (!b.hasTagCompound() || a.hasTagCompound() && NBTTagCompound_areEqual(b.getTagCompound(), a.getTagCompound()));
    }

    private static boolean NBTTagCompound_areEqual(NBTTagCompound expected, NBTTagCompound actual) {
        return legacyconnector.v1_12_2.net.minecraft.nbt.NBTUtil.areNBTEquals(expected, actual, false);
    }

    public void writeToBuf(PacketBuffer buffer) {
        buffer.writeByte((byte) (size() & 255));
        for (MerchantRecipe r : this) {
            buffer.writeItemStack(r.getItemToBuy());
            buffer.writeItemStack(r.getItemToSell());
            ItemStack second = r.getSecondItemToBuy();
            buffer.writeBoolean(!second.isEmpty());
            if (!second.isEmpty()) buffer.writeItemStack(second);
            buffer.writeBoolean(r.isRecipeDisabled());
            buffer.writeInt(r.getToolUses());
            buffer.writeInt(r.getMaxTradeUses());
        }
    }

    public static MerchantRecipeList readFromBuf(PacketBuffer buffer) throws IOException {
        MerchantRecipeList list = new MerchantRecipeList();
        int n = buffer.readByte() & 255;
        for (int i = 0; i < n; ++i) {
            ItemStack buy = buffer.readItemStack();
            ItemStack sell = buffer.readItemStack();
            ItemStack second = buffer.readBoolean() ? buffer.readItemStack() : ItemStack.EMPTY;
            boolean disabled = buffer.readBoolean();
            int uses = buffer.readInt();
            int max = buffer.readInt();
            MerchantRecipe r = new MerchantRecipe(buy, second, sell, uses, max);
            if (disabled) r.compensateToolUses();
            list.add(r);
        }
        return list;
    }

    public void readRecipiesFromTags(NBTTagCompound tag) {
        NBTTagList recipes = tag.getTagList("Recipes", 10);
        for (int i = 0; i < recipes.tagCount(); ++i) add(new MerchantRecipe(recipes.getCompoundTagAt(i)));
    }

    public NBTTagCompound getRecipiesAsTags() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList recipes = new NBTTagList();
        for (MerchantRecipe r : this) recipes.appendTag(r.writeToTags());
        tag.setTag("Recipes", recipes);
        return tag;
    }

    /** The modern offers for these trades (trades whose items have no modern counterpart are left out). */
    public net.minecraft.world.item.trading.MerchantOffers mcc$toModern() {
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        for (MerchantRecipe r : this) {
            var offer = r.mcc$toModern();
            if (offer != null) offers.add(offer);
        }
        return offers;
    }
}
