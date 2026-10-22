package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public class ItemFood extends Item {
    public final int itemUseDuration;
    private final int healAmount;
    private final float saturationModifier;
    private final boolean isWolfsFavoriteMeat;
    private boolean alwaysEdible;

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

    public ItemFood setAlwaysEdible() {
        alwaysEdible = true;
        return this;
    }

    public boolean mcc$alwaysEdible() {
        return alwaysEdible;
    }
}
