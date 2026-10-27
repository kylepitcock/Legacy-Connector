package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.Random;

/** 1.12.2 loot number range (a constant when min equals max); {@link #mcc$modern} is the modern number provider. */
public class RandomValueRange {
    private final float min;
    private final float max;

    public RandomValueRange(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public RandomValueRange(float value) {
        this(value, value);
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public int generateInt(Random rand) {
        int lo = (int) Math.floor(min), hi = (int) Math.floor(max);
        return lo >= hi ? lo : rand.nextInt(hi - lo + 1) + lo;
    }

    public float generateFloat(Random rand) {
        return min >= max ? min : rand.nextFloat() * (max - min) + min;
    }

    public boolean isInRange(int value) {
        return value <= max && value >= min;
    }

    public net.minecraft.world.level.storage.loot.providers.number.NumberProvider mcc$modern() {
        return min == max ? net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(min)
                : net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(min, max);
    }

    /** Gson adapter for loot table JSON ({@code 3} or {@code {"min": 1, "max": 3}}). */
    public static class Serializer implements com.google.gson.JsonDeserializer<RandomValueRange>, com.google.gson.JsonSerializer<RandomValueRange> {
        @Override
        public RandomValueRange deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            if (json.isJsonPrimitive()) return new RandomValueRange(json.getAsFloat());
            var o = json.getAsJsonObject();
            return new RandomValueRange(o.get("min").getAsFloat(), o.get("max").getAsFloat());
        }

        @Override
        public com.google.gson.JsonElement serialize(RandomValueRange range, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext context) {
            if (range.min == range.max) return new com.google.gson.JsonPrimitive(range.min);
            var o = new com.google.gson.JsonObject();
            o.addProperty("min", range.min);
            o.addProperty("max", range.max);
            return o;
        }
    }
}
