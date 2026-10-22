package legacyconnector.v1_7_10.net.minecraft.item;

/**
 * Vanilla 1.7.10 armor: armorType 0 helmet, 1 chestplate, 2 leggings, 3 boots. Legacy armor is wearable in that
 * slot and gives its armor points (see LegacyItemAdapter / ItemBridge).
 */
public class ItemArmor extends legacyconnector.v1_7_10.net.minecraft.item.Item {
    public final int armorType;
    public final int damageReduceAmount;
    public final int renderIndex;
    private final ArmorMaterial material;

    public ItemArmor(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        int type = 1;
        int defense = 0;
        if (info.modern() instanceof net.minecraft.world.item.ArmorItem a) {
            type = switch (a.getType()) {
                case HELMET -> 0;
                case CHESTPLATE, BODY -> 1;
                case LEGGINGS -> 2;
                case BOOTS -> 3;
            };
            defense = a.getDefense();
        }
        this.armorType = type;
        this.damageReduceAmount = defense;
        this.renderIndex = 0;
        this.material = ArmorMaterial.IRON;
    }

    protected ItemArmor() {
        this(ArmorMaterial.IRON, 0, 1);
    }

    public ItemArmor(ArmorMaterial material, int renderIndex, int armorType) {
        super();
        this.material = material == null ? ArmorMaterial.IRON : material;
        this.armorType = armorType;
        this.renderIndex = renderIndex;
        this.damageReduceAmount = armorType >= 0 && armorType < 4 ? this.material.getDamageReductionAmount(armorType) : 0;
        this.setMaxDamage(armorType >= 0 && armorType < 4 ? this.material.getDurability(armorType) : 0);
        this.maxStackSize = 1;
    }

    public ArmorMaterial getArmorMaterial() {
        return material;
    }

    @Override
    public int getItemEnchantability() {
        return material.getEnchantability();
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        Item base = material.getBaseItemForRepair();
        return base != null && repair != null && repair.getItem() == base || super.getIsRepairable(toRepair, repair);
    }

    /** Forge: texture of the worn armor ("modid:textures/models/armor/x_layer_1.png"), or null for the default. */
    public String getArmorTexture(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, int slot, String type) {
        return null;
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
        public legacyconnector.v1_7_10.net.minecraft.item.Item customCraftingMaterial = null;

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

        public legacyconnector.v1_7_10.net.minecraft.item.Item getBaseItemForRepair() {
            // No switch: EnumHelper appends constants after javac's switch map was built.
            if (this == CLOTH) return legacyconnector.v1_7_10.net.minecraft.init.Items.leather;
            if (this == CHAIN || this == IRON) return legacyconnector.v1_7_10.net.minecraft.init.Items.iron_ingot;
            if (this == GOLD) return legacyconnector.v1_7_10.net.minecraft.init.Items.gold_ingot;
            if (this == DIAMOND) return legacyconnector.v1_7_10.net.minecraft.init.Items.diamond;
            return customCraftingMaterial;
        }
    }
}
