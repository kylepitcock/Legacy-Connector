package legacyconnector.v1_6_4.facade.net.minecraft.village;

import legacyconnector.v1_6_4.forward.Forward164;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe;

/** Facade of the 1.7.10 shim's {@code MerchantRecipeList} for 1.6.4 mods: the stream form of the trade list (item stacks in the 1.6.4 wire format). Never instantiated. */
public final class MerchantRecipeList {
    private MerchantRecipeList() {}

    /** 1.6.4 {@code writeRecipiesToStream(out)}: a count byte, then buy, sell, an optional second buy and the disabled flag per trade. */
    public static void writeRecipiesToStream(legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList self, java.io.DataOutputStream out) throws java.io.IOException {
        out.writeByte((byte) (self.size() & 255));
        for (Object o : self) {
            MerchantRecipe r = (MerchantRecipe) o;
            Forward164.writeItemStack(r.getItemToBuy(), out);
            Forward164.writeItemStack(r.getItemToSell(), out);
            ItemStack second = r.getSecondItemToBuy();
            out.writeBoolean(second != null);
            if (second != null) Forward164.writeItemStack(second, out);
            out.writeBoolean(r.isRecipeDisabled());
        }
    }

    /** 1.6.4 {@code readRecipiesFromStream(in)}: the trade list {@link #writeRecipiesToStream} wrote. */
    public static legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList readRecipiesFromStream(java.io.DataInputStream in) throws java.io.IOException {
        legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList list = new legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList();
        int count = in.readByte() & 255;
        for (int i = 0; i < count; i++) {
            ItemStack buy = Forward164.readItemStack(in);
            ItemStack sell = Forward164.readItemStack(in);
            ItemStack second = in.readBoolean() ? Forward164.readItemStack(in) : null;
            boolean disabled = in.readBoolean();
            MerchantRecipe r = new MerchantRecipe(buy, second, sell);
            if (disabled && !r.isRecipeDisabled()) r.func_82785_h();
            list.add(r);
        }
        return list;
    }
}
