package legacyconnector.v1_12_2.net.minecraftforge.advancements.critereon;

import legacyconnector.v1_12_2.net.minecraft.advancements.critereon.ItemPredicate;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary;

/** Forge's {@code forge:ore_dict} item predicate: a stack registered under the ore name. */
public class OredictItemPredicate extends ItemPredicate {
    private final String ore;

    public OredictItemPredicate(String ore) {
        this.ore = ore;
    }

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && OreDictionary.containsMatch(false, OreDictionary.getOres(ore), stack);
    }
}
