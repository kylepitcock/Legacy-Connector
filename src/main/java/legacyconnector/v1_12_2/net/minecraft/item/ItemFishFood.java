package legacyconnector.v1_12_2.net.minecraft.item;

import java.util.HashMap;
import java.util.Map;

/** Vanilla's raw/cooked fish item: one subtype per {@link FishType}, whose metadata is the stack's. */
public class ItemFishFood extends ItemFood {
    private final boolean cooked;

    public ItemFishFood(boolean cooked) {
        super(0, 0.0F, false);
        this.cooked = cooked;
    }

    @Override
    public int getHealAmount(ItemStack stack) {
        FishType type = FishType.byItemStack(stack);
        return cooked && type.canCook() ? type.getCookedHealAmount() : type.getUncookedHealAmount();
    }

    @Override
    public float getSaturationModifier(ItemStack stack) {
        FishType type = FishType.byItemStack(stack);
        return cooked && type.canCook() ? type.getCookedSaturationModifier() : type.getUncookedSaturationModifier();
    }

    public enum FishType {
        COD(0, "cod", 2, 0.1F, 5, 0.6F),
        SALMON(1, "salmon", 2, 0.1F, 6, 0.8F),
        CLOWNFISH(2, "clownfish", 1, 0.1F),
        PUFFERFISH(3, "pufferfish", 1, 0.1F);

        private static final Map<Integer, FishType> META_LOOKUP = new HashMap<>();
        private final int meta;
        private final String translationKey;
        private final int uncookedHealAmount;
        private final float uncookedSaturationModifier;
        private final int cookedHealAmount;
        private final float cookedSaturationModifier;
        private final boolean cookable;

        static {
            for (FishType type : values()) META_LOOKUP.put(type.meta, type);
        }

        FishType(int meta, String key, int rawHeal, float rawSaturation, int cookedHeal, float cookedSaturation) {
            this.meta = meta;
            this.translationKey = key;
            this.uncookedHealAmount = rawHeal;
            this.uncookedSaturationModifier = rawSaturation;
            this.cookedHealAmount = cookedHeal;
            this.cookedSaturationModifier = cookedSaturation;
            this.cookable = true;
        }

        FishType(int meta, String key, int rawHeal, float rawSaturation) {
            this.meta = meta;
            this.translationKey = key;
            this.uncookedHealAmount = rawHeal;
            this.uncookedSaturationModifier = rawSaturation;
            this.cookedHealAmount = 0;
            this.cookedSaturationModifier = 0.0F;
            this.cookable = false;
        }

        public int getMetadata() {
            return meta;
        }

        public String getTranslationKey() {
            return translationKey;
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

        public boolean canCook() {
            return cookable;
        }

        /** The type with this metadata; cod for unknown values. */
        public static FishType byMetadata(int meta) {
            FishType type = META_LOOKUP.get(meta);
            return type == null ? COD : type;
        }

        /** The type of a fish stack (vanilla's fish items are modern items here, not this class); cod for other items. */
        public static FishType byItemStack(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return COD;
            Item item = stack.getItem();
            boolean fish = item instanceof ItemFishFood || item == legacyconnector.v1_12_2.net.minecraft.init.Items.FISH
                    || item == legacyconnector.v1_12_2.net.minecraft.init.Items.COOKED_FISH;
            return fish ? byMetadata(stack.getMetadata()) : COD;
        }
    }
}
