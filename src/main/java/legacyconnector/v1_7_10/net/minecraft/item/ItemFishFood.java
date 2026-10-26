package legacyconnector.v1_7_10.net.minecraft.item;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** Vanilla's raw/cooked fish item: one subtype per {@link FishType}, whose metadata is the item damage. */
public class ItemFishFood extends ItemFood {
    private final boolean cooked;

    public ItemFishFood(boolean cooked) {
        super(0, 0.0F, false);
        this.cooked = cooked;
    }

    @Override
    public int getHealAmount(ItemStack stack) {
        FishType type = FishType.func_150978_a(stack);
        return cooked && type.getCookable() ? type.getCookedHealAmount() : type.getUncookedHealAmount();
    }

    @Override
    public float getSaturationModifier(ItemStack stack) {
        FishType type = FishType.func_150978_a(stack);
        return cooked && type.getCookable() ? type.getCookedSaturationModifier() : type.getUncookedSaturationModifier();
    }

    public enum FishType {
        COD(0, "cod", 2, 0.1F, 5, 0.6F),
        SALMON(1, "salmon", 2, 0.1F, 6, 0.8F),
        CLOWNFISH(2, "clownfish", 1, 0.1F),
        PUFFERFISH(3, "pufferfish", 1, 0.1F);

        private static final Map<Integer, FishType> itemDamageToFishTypeMap = new HashMap<>();
        private final int itemDamage;
        private final String unlocalizedNamePart;
        private IIcon uncookedIcon;
        private IIcon cookedIcon;
        private final int uncookedHealAmount;
        private final float uncookedSaturationModifier;
        private final int cookedHealAmount;
        private final float cookedSaturationModifier;
        private final boolean cookable;

        static {
            for (FishType type : values()) itemDamageToFishTypeMap.put(type.itemDamage, type);
        }

        FishType(int damage, String name, int rawHeal, float rawSaturation, int cookedHeal, float cookedSaturation) {
            this.itemDamage = damage;
            this.unlocalizedNamePart = name;
            this.uncookedHealAmount = rawHeal;
            this.uncookedSaturationModifier = rawSaturation;
            this.cookedHealAmount = cookedHeal;
            this.cookedSaturationModifier = cookedSaturation;
            this.cookable = true;
        }

        FishType(int damage, String name, int rawHeal, float rawSaturation) {
            this.itemDamage = damage;
            this.unlocalizedNamePart = name;
            this.uncookedHealAmount = rawHeal;
            this.uncookedSaturationModifier = rawSaturation;
            this.cookedHealAmount = 0;
            this.cookedSaturationModifier = 0.0F;
            this.cookable = false;
        }

        public int getItemDamage() {
            return itemDamage;
        }

        public String getUnlocalizedNamePart() {
            return unlocalizedNamePart;
        }

        public int getUncookedHealAmount() {
            return uncookedHealAmount;
        }

        public float getUncookedSaturationModifier() {
            return uncookedSaturationModifier;
        }

        public int getCookedHealAmount() {
            return cookedHealAmount;
        }

        public float getCookedSaturationModifier() {
            return cookedSaturationModifier;
        }

        public void registerIcon(IIconRegister register) {
            uncookedIcon = register.registerIcon("fish_" + unlocalizedNamePart + "_raw");
            if (cookable) cookedIcon = register.registerIcon("fish_" + unlocalizedNamePart + "_cooked");
        }

        public IIcon getUncookedIcon() {
            return uncookedIcon;
        }

        public IIcon getCookedIcon() {
            return cookedIcon;
        }

        public boolean getCookable() {
            return cookable;
        }

        /** The type with this item damage; cod for unknown values. */
        public static FishType getFishTypeForItemDamage(int damage) {
            FishType type = itemDamageToFishTypeMap.get(damage);
            return type == null ? COD : type;
        }

        /** The type of a fish stack (vanilla's fish items are modern items here, not this class); cod for other items. */
        public static FishType func_150978_a(ItemStack stack) {
            Item item = stack == null ? null : stack.getItem();
            boolean fish = item instanceof ItemFishFood || item != null && (item == legacyconnector.v1_7_10.net.minecraft.init.Items.fish
                    || item == legacyconnector.v1_7_10.net.minecraft.init.Items.cooked_fished);
            return fish ? getFishTypeForItemDamage(stack.getItemDamage()) : COD;
        }
    }
}
