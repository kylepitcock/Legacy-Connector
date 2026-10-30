package legacyconnector.v1_12_2.net.minecraft.item;

/** 1.12.2's splash potion: thrown (the modern item throws it), named "Splash Potion of ...". */
public class ItemSplashPotion extends ItemPotion {
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n.translateToLocal(
                legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.getPotionFromItem(stack).getNamePrefixed("splash_potion.effect."));
    }
}
