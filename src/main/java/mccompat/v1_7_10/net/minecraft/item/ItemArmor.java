package mccompat.v1_7_10.net.minecraft.item;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.item.ItemArmor}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class ItemArmor extends mccompat.v1_7_10.net.minecraft.item.Item {
    public ItemArmor(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected ItemArmor() {
        super();
    }

    /** Armor tiers. Mods add their own through {@code EnumHelper.addArmorMaterial}. */
    public enum ArmorMaterial {
        CLOTH(5, new int[] {1, 3, 2, 1}, 15),
        CHAIN(15, new int[] {2, 5, 4, 1}, 12),
        IRON(15, new int[] {2, 6, 5, 2}, 9),
        GOLD(7, new int[] {2, 5, 3, 1}, 25),
        DIAMOND(33, new int[] {3, 8, 6, 3}, 10);

        private static final int[] MAX_DAMAGE_ARRAY = {11, 16, 15, 13};
        private final int maxDamageFactor;
        private final int[] damageReductionAmountArray;
        private final int enchantability;
        /** Forge: repair item of mod materials. */
        public mccompat.v1_7_10.net.minecraft.item.Item customCraftingMaterial = null;

        ArmorMaterial(int maxDamageFactor, int[] reduction, int enchantability) {
            this.maxDamageFactor = maxDamageFactor;
            this.damageReductionAmountArray = reduction;
            this.enchantability = enchantability;
        }

        public int getDurability(int armorType) {
            return MAX_DAMAGE_ARRAY[armorType] * maxDamageFactor;
        }

        public int getDamageReductionAmount(int armorType) {
            return damageReductionAmountArray[armorType];
        }

        public int getEnchantability() {
            return enchantability;
        }

        public mccompat.v1_7_10.net.minecraft.item.Item getBaseItemForRepair() {
            // No switch: EnumHelper appends constants after javac's switch map was built.
            if (this == CLOTH) return mccompat.v1_7_10.net.minecraft.init.Items.leather;
            if (this == CHAIN || this == IRON) return mccompat.v1_7_10.net.minecraft.init.Items.iron_ingot;
            if (this == GOLD) return mccompat.v1_7_10.net.minecraft.init.Items.gold_ingot;
            if (this == DIAMOND) return mccompat.v1_7_10.net.minecraft.init.Items.diamond;
            return customCraftingMaterial;
        }
    }
}
