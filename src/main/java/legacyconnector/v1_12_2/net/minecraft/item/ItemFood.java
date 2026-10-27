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

    private legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect potionId;
    private float potionEffectProbability;

    /** An effect eating gives, with a chance (rotten flesh's hunger). */
    public ItemFood setPotionEffect(legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect effect, float probability) {
        this.potionId = effect;
        this.potionEffectProbability = probability;
        return this;
    }

    public legacyconnector.v1_12_2.net.minecraft.potion.PotionEffect mcc$potionEffect() {
        return potionId;
    }

    public float mcc$potionEffectProbability() {
        return potionEffectProbability;
    }

    /** After eating (the modern food component gave hunger, saturation and the potion effect): mods add their own effects. */
    protected void onFoodEaten(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
    }

    public void mcc$onFoodEaten(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.world.World world,
            legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        onFoodEaten(stack, world, player);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return itemUseDuration;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.EAT;
    }
}
