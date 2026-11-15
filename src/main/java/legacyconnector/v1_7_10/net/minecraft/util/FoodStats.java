package legacyconnector.v1_7_10.net.minecraft.util;

import legacyconnector.v1_7_10.net.minecraft.item.ItemFood;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.world.food.FoodData;

/** View over the player's modern {@link FoodData}. */
public class FoodStats {
    private final FoodData mcc$food;

    /** Phantom fields: accesses are redirected to the {@code mcc$get$}/{@code mcc$set$} accessors below. */
    @Deprecated
    public int foodLevel;
    @Deprecated
    public float foodSaturationLevel;
    @Deprecated
    public float foodExhaustionLevel;
    @Deprecated
    public int foodTimer;
    @Deprecated
    public int prevFoodLevel;

    public static int mcc$get$foodLevel(FoodStats s) {
        return s.mcc$food.getFoodLevel();
    }

    public static void mcc$set$foodLevel(FoodStats s, int v) {
        s.mcc$food.setFoodLevel(v);
    }

    public static float mcc$get$foodSaturationLevel(FoodStats s) {
        return s.mcc$food.getSaturationLevel();
    }

    public static void mcc$set$foodSaturationLevel(FoodStats s, float v) {
        s.mcc$food.setSaturation(v);
    }

    public static float mcc$get$foodExhaustionLevel(FoodStats s) {
        return s.mcc$food.getExhaustionLevel();
    }

    public static void mcc$set$foodExhaustionLevel(FoodStats s, float v) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        s.mcc$food.addAdditionalSaveData(tag);
        tag.putFloat("foodExhaustionLevel", v);
        s.mcc$food.readAdditionalSaveData(tag);
    }

    public static int mcc$get$foodTimer(FoodStats s) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        s.mcc$food.addAdditionalSaveData(tag);
        return tag.getInt("foodTickTimer");
    }

    public static void mcc$set$foodTimer(FoodStats s, int v) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        s.mcc$food.addAdditionalSaveData(tag);
        tag.putInt("foodTickTimer", v);
        s.mcc$food.readAdditionalSaveData(tag);
    }

    public static int mcc$get$prevFoodLevel(FoodStats s) {
        return s.mcc$food.getLastFoodLevel();
    }

    /** The modern game records the previous level itself at the start of each tick. */
    public static void mcc$set$prevFoodLevel(FoodStats s, int v) {
    }

    public FoodStats(FoodData food) {
        this.mcc$food = food;
    }

    public FoodData mcc$modern() {
        return mcc$food;
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
