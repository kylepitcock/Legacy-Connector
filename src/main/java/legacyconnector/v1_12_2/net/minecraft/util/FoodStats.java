package legacyconnector.v1_12_2.net.minecraft.util;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * A player's hunger: a view over the modern player's food data (the same rules), or plain values for a standalone instance.
 * The fields mods read directly are phantom fields answered from the modern data.
 */
public class FoodStats {
    private final net.minecraft.world.food.FoodData modern;

    /** Phantom fields: accesses are redirected to the {@code mcc$get$}/{@code mcc$set$} accessors below. */
    @Deprecated
    public int foodLevel;
    @Deprecated
    public float foodSaturationLevel;
    @Deprecated
    public float foodExhaustionLevel;

    public FoodStats() {
        this(new net.minecraft.world.food.FoodData());
    }

    protected FoodStats(net.minecraft.world.food.FoodData modern) {
        this.modern = modern;
    }

    /** The legacy view of a modern player's food data. */
    public static FoodStats mcc$of(net.minecraft.world.food.FoodData modern) {
        return new FoodStats(modern);
    }

    public net.minecraft.world.food.FoodData mcc$modern() {
        return modern;
    }

    public static int mcc$get$foodLevel(FoodStats s) {
        return s.modern.getFoodLevel();
    }

    public static void mcc$set$foodLevel(FoodStats s, int v) {
        s.modern.setFoodLevel(v);
    }

    public static float mcc$get$foodSaturationLevel(FoodStats s) {
        return s.modern.getSaturationLevel();
    }

    public static void mcc$set$foodSaturationLevel(FoodStats s, float v) {
        s.modern.setSaturation(v);
    }

    public static float mcc$get$foodExhaustionLevel(FoodStats s) {
        return s.modern.getExhaustionLevel();
    }

    public static void mcc$set$foodExhaustionLevel(FoodStats s, float v) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        s.modern.addAdditionalSaveData(tag);
        tag.putFloat("foodExhaustionLevel", v);
        s.modern.readAdditionalSaveData(tag);
    }

    public void addStats(int foodLevelIn, float foodSaturationModifier) {
        modern.eat(foodLevelIn, foodSaturationModifier);
    }

    public void addStats(legacyconnector.v1_12_2.net.minecraft.item.ItemFood foodItem, legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        addStats(foodItem.getHealAmount(stack), foodItem.getSaturationModifier(stack));
    }

    public void onUpdate(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player) {
        if (player != null && player.mcc$player() != null) modern.tick(player.mcc$player());
    }

    public void readNBT(NBTTagCompound compound) {
        if (compound.hasKey("foodLevel", 99)) modern.readAdditionalSaveData(compound.mcc$modern());
    }

    public void writeNBT(NBTTagCompound compound) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        modern.addAdditionalSaveData(tag);
        for (String key : tag.getAllKeys()) compound.setTag(key, legacyconnector.v1_12_2.net.minecraft.nbt.NBTBase.mcc$wrap(tag.get(key), null));
    }

    public int getFoodLevel() {
        return modern.getFoodLevel();
    }

    public boolean needFood() {
        return modern.needsFood();
    }

    public void addExhaustion(float exhaustion) {
        modern.addExhaustion(exhaustion);
    }

    public float getSaturationLevel() {
        return modern.getSaturationLevel();
    }

    public void setFoodLevel(int foodLevelIn) {
        modern.setFoodLevel(foodLevelIn);
    }

    public void setFoodSaturationLevel(float foodSaturationLevelIn) {
        modern.setSaturation(foodSaturationLevelIn);
    }
}
