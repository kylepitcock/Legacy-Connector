package legacyconnector.v1_12_2.net.minecraft.item;

public class ItemFood extends Item {
    public final int itemUseDuration = 32;
    private final int healAmount;
    private final float saturationModifier;
    private final boolean isWolfsFavoriteMeat;
    private boolean alwaysEdible;

    public ItemFood(int amount, float saturation, boolean isWolfFood) {
        this.healAmount = amount;
        this.saturationModifier = saturation;
        this.isWolfsFavoriteMeat = isWolfFood;
    }

    public ItemFood(int amount, boolean isWolfFood) {
        this(amount, 0.6F, isWolfFood);
    }

    public int getHealAmount(ItemStack stack) {
        return healAmount;
    }

    public float getSaturationModifier(ItemStack stack) {
        return saturationModifier;
    }

    public boolean isWolfsFavoriteMeat() {
        return isWolfsFavoriteMeat;
    }

    public ItemFood setAlwaysEdible() {
        this.alwaysEdible = true;
        return this;
    }

    public boolean mcc$alwaysEdible() {
        return alwaysEdible;
    }
}
