package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.ArrayList;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** The trades of a villager. */
public class MerchantRecipeList extends ArrayList {
    public MerchantRecipeList() {}

    public MerchantRecipeList(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        readRecipiesFromTags(tag);
    }

    /** Adds the trades saved under {@code Recipes} (villagers' {@code Offers} tag). */
    public void readRecipiesFromTags(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        var list = tag.getTagList("Recipes", 10);
        for (int i = 0; i < list.tagCount(); i++) add(new MerchantRecipe(list.getCompoundTagAt(i)));
    }

    /** The trades as saved by 1.7.10: a {@code Recipes} list of {@link MerchantRecipe#writeToTags} compounds. */
    public legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound getRecipiesAsTags() {
        var tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        var list = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (Object o : this) list.appendTag(((MerchantRecipe) o).writeToTags());
        tag.setTag("Recipes", list);
        return tag;
    }

    public MerchantRecipe canRecipeBeUsed(ItemStack buy, ItemStack secondBuy, int index) {
        if (index > 0 && index < size()) {
            MerchantRecipe r = (MerchantRecipe) get(index);
            return matches(r, buy, secondBuy) ? r : null;
        }
        for (Object o : this) {
            MerchantRecipe r = (MerchantRecipe) o;
            if (matches(r, buy, secondBuy)) return r;
        }
        return null;
    }

    private static boolean matches(MerchantRecipe r, ItemStack buy, ItemStack secondBuy) {
        if (r.isRecipeDisabled() || buy == null || r.getItemToBuy() == null) return false;
        if (buy.getItem() != r.getItemToBuy().getItem() || ItemStack.mcc$get$stackSize(buy) < ItemStack.mcc$get$stackSize(r.getItemToBuy())) return false;
        if (!r.hasSecondItemToBuy()) return secondBuy == null;
        return secondBuy != null && secondBuy.getItem() == r.getSecondItemToBuy().getItem() && ItemStack.mcc$get$stackSize(secondBuy) >= ItemStack.mcc$get$stackSize(r.getSecondItemToBuy());
    }

    public void addToListWithCheck(MerchantRecipe recipe) {
        for (Object o : this) {
            MerchantRecipe r = (MerchantRecipe) o;
            if (r.getItemToBuy() != null && recipe.getItemToBuy() != null && r.getItemToBuy().getItem() == recipe.getItemToBuy().getItem()
                    && r.getItemToSell() != null && recipe.getItemToSell() != null && r.getItemToSell().getItem() == recipe.getItemToSell().getItem()) {
                return;
            }
        }
        add(recipe);
    }

    /** {@code func_151391_a}: writes the trades to a packet buffer (a count byte, then buy, sell, an optional second buy, the disabled flag, uses and maximum). */
    public void func_151391_a(legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer buf) {
        try {
            buf.writeByte((byte) (size() & 255));
            for (Object o : this) {
                MerchantRecipe r = (MerchantRecipe) o;
                buf.writeItemStackToBuffer(r.getItemToBuy());
                buf.writeItemStackToBuffer(r.getItemToSell());
                ItemStack second = r.getSecondItemToBuy();
                buf.writeBoolean(second != null);
                if (second != null) buf.writeItemStackToBuffer(second);
                buf.writeBoolean(r.isRecipeDisabled());
                buf.writeInt(r.getToolUses());
                buf.writeInt(r.getMaxTradeUses());
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@code func_151390_b}: reads trades written by {@link #func_151391_a}. */
    public static MerchantRecipeList func_151390_b(legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer buf) {
        MerchantRecipeList list = new MerchantRecipeList();
        try {
            int count = buf.readByte() & 255;
            for (int i = 0; i < count; i++) {
                ItemStack buy = buf.readItemStackFromBuffer();
                ItemStack sell = buf.readItemStackFromBuffer();
                ItemStack second = buf.readBoolean() ? buf.readItemStackFromBuffer() : null;
                boolean disabled = buf.readBoolean();
                int uses = buf.readInt();
                int max = buf.readInt();
                MerchantRecipe r = new MerchantRecipe(buy, second, sell);
                r.mcc$setUses(uses, max);
                if (disabled && !r.isRecipeDisabled()) r.func_82785_h();
                list.add(r);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
