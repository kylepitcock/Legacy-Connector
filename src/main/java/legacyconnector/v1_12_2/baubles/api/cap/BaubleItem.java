package legacyconnector.v1_12_2.baubles.api.cap;

import legacyconnector.v1_12_2.baubles.api.BaubleType;
import legacyconnector.v1_12_2.baubles.api.IBauble;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Baubles' default bauble capability instance: a bauble of one type with the default behaviour. */
public class BaubleItem implements IBauble {
    private final BaubleType baubleType;

    public BaubleItem(BaubleType type) {
        this.baubleType = type;
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return baubleType;
    }
}
