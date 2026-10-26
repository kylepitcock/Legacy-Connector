package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.item.ItemFood;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.food.FoodData;

/** View over the player's modern {@link FoodData}. */
public class FoodStats {
    private final FoodData mcc$food;

    public FoodStats(FoodData food) {
        this.mcc$food = food;
    }

    /** Stand-alone food stats (full: 20 food, 5 saturation), as mods create to save and restore a player's hunger. */
    public FoodStats() {
        this(new FoodData());
    }

    /** Reads {@code foodLevel}, {@code foodTickTimer}, {@code foodSaturationLevel} and {@code foodExhaustionLevel} (the same keys today). */
    public void readNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (tag != null) mcc$food.readAdditionalSaveData(tag.mcc$modern());
    }

    public void writeNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        if (tag == null) return;
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        mcc$food.addAdditionalSaveData(saved);
        tag.setInteger("foodLevel", saved.getInt("foodLevel"));
        tag.setInteger("foodTickTimer", saved.getInt("foodTickTimer"));
        tag.setFloat("foodSaturationLevel", saved.getFloat("foodSaturationLevel"));
        tag.setFloat("foodExhaustionLevel", saved.getFloat("foodExhaustionLevel"));
    }

    public void addStats(int food, float saturation) {
        mcc$food.eat(food, saturation);
    }

    public void addStats(ItemFood item, ItemStack stack) {
        addStats(item.getHealAmount(stack), item.getSaturationModifier(stack));
    }

    public int getFoodLevel() {
        return mcc$food.getFoodLevel();
    }

    public int getPrevFoodLevel() {
        return mcc$food.getLastFoodLevel();
    }

    public boolean needFood() {
        return mcc$food.needsFood();
    }

    public void addExhaustion(float amount) {
        mcc$food.addExhaustion(amount);
    }

    public float getSaturationLevel() {
        return mcc$food.getSaturationLevel();
    }

    public void setFoodLevel(int level) {
        mcc$food.setFoodLevel(level);
    }

    public void setFoodSaturationLevel(float level) {
        mcc$food.setSaturation(level);
    }
}
