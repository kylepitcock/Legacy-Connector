package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public class ItemFood extends Item {
    public final int itemUseDuration;
    private final int healAmount;
    private final float saturationModifier;
    private final boolean isWolfsFavoriteMeat;
    private boolean alwaysEdible;

    /**
     * A vanilla food as 1.7.10 mods see it (so that {@code instanceof ItemFood} holds: Witchery's oven only cooks food, hunger and
     * cooking mods test it); values come from the modern item's food component.
     */
    public ItemFood(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
        net.minecraft.world.item.Item modern = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(info.legacyName()));
        net.minecraft.world.food.FoodProperties food = modern == null ? null : modern.components().get(net.minecraft.core.component.DataComponents.FOOD);
        this.itemUseDuration = 32;
        this.healAmount = food == null ? 0 : food.nutrition();
        this.saturationModifier = food == null || food.nutrition() <= 0 ? 0.6F : food.saturation() / (2.0F * food.nutrition());
        this.isWolfsFavoriteMeat = info.legacyName().endsWith("beef") || info.legacyName().endsWith("porkchop") || info.legacyName().endsWith("chicken") || info.legacyName().endsWith("rotten_flesh");
    }

    public ItemFood(int heal, float saturation, boolean wolfMeat) {
        this.itemUseDuration = 32;
        this.healAmount = heal;
        this.saturationModifier = saturation;
        this.isWolfsFavoriteMeat = wolfMeat;
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabFood);
    }

    public ItemFood(int heal, boolean wolfMeat) {
        this(heal, 0.6F, wolfMeat);
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        stack.mcc$setCount(stack.mcc$count() - 1);
        player.getFoodStats().addStats(this, stack);
        world.playSoundAtEntity(player, "random.burp", 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
        if (potionId > 0 && !world.isRemote && world.rand.nextFloat() < potionEffectProbability) {
            player.addPotionEffect(new legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect(potionId, potionDuration * 20, potionAmplifier));
        }
        onFoodEaten(stack, world, player);
        return stack;
    }

    protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.eat;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.canEat(alwaysEdible)) player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
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

    private int potionId;
    private int potionDuration;
    private int potionAmplifier;
    private float potionEffectProbability;

    public ItemFood setPotionEffect(int id, int seconds, int amplifier, float probability) {
        potionId = id;
        potionDuration = seconds;
        potionAmplifier = amplifier;
        potionEffectProbability = probability;
        return this;
    }

    public ItemFood setAlwaysEdible() {
        alwaysEdible = true;
        return this;
    }

    public boolean mcc$alwaysEdible() {
        return alwaysEdible;
    }
}
