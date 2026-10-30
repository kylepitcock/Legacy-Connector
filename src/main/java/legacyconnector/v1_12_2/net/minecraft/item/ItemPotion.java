package legacyconnector.v1_12_2.net.minecraft.item;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionType;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/**
 * 1.12.2's potion item: one item, the potion type in the stack's "Potion" tag (water by default). Drinking is the modern potion's (the
 * vanilla item is the modern one); mods read and make potion stacks through this class and {@link PotionUtils}.
 */
public class ItemPotion extends Item {
    public ItemPotion() {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.BREWING);
    }

    @Override
    public ItemStack getDefaultInstance() {
        return PotionUtils.addPotionToItemStack(super.getDefaultInstance(), PotionType.mcc$vanilla("water"));
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    /** The name of the potion type ("Potion of Swiftness"). */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return legacyconnector.v1_12_2.net.minecraft.util.text.translation.I18n
                .translateToLocal(PotionUtils.getPotionFromItem(stack).getNamePrefixed("potion.effect."));
    }

    @Override
    public void addInformation(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World worldIn, List<String> tooltip,
            legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flagIn) {
        PotionUtils.addPotionTooltip(stack, tooltip, 1.0F);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return super.hasEffect(stack) || !PotionUtils.getEffectsFromStack(stack).isEmpty();
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        for (PotionType type : PotionType.REGISTRY) {
            if (type != PotionType.mcc$vanilla("empty")) items.add(PotionUtils.addPotionToItemStack(new ItemStack(this), type));
        }
    }
}
