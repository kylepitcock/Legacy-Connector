package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2's lingering potion: thrown (the modern item throws it), named "Lingering Potion of ...". */
public class ItemLingeringPotion extends ItemPotion {
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(
                legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.getPotionFromItem(stack).getNamePrefixed("lingering_potion.effect."));
    }
}
