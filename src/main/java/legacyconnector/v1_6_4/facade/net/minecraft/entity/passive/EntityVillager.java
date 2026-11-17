package legacyconnector.v1_6_4.facade.net.minecraft.entity.passive;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;

/** Facade of the 1.7.10 shim's {@code EntityVillager} for 1.6.4 mods: the trade helpers that took item ids. Never instantiated. */
public final class EntityVillager {
    private EntityVillager() {}

    /** 1.6.4 {@code addBlacksmithItem(list, itemId, random, chance)}: with {@code chance} a trade where the villager sells the item for emeralds (1.7.10: func_146089_b). */
    public static void addBlacksmithItem(legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList list, int itemId, java.util.Random random, float chance) {
        legacyconnector.v1_7_10.net.minecraft.item.Item item = Ids.item(itemId);
        if (item != null) legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager.func_146089_b(list, item, random, chance);
    }
}
