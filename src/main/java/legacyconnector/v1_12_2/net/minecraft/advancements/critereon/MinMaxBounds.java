package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import legacyconnector.v1_12_2.net.minecraft.util.JsonUtils;

/** A number range of a criterion condition: a value, or {@code {"min":..,"max":..}} (either side open). */
public class MinMaxBounds {
    public static final MinMaxBounds UNBOUNDED = new MinMaxBounds(null, null);
    private final Float min;
    private final Float max;

    public MinMaxBounds(Float min, Float max) {
        this.min = min;
        this.max = max;
    }

    public boolean test(float value) {
        if (min != null && min > value) return false;
        return max == null || max >= value;
    }

    public boolean testSquare(double value) {
        if (min != null && (double) (min * min) > value) return false;
        return max == null || (double) (max * max) >= value;
    }

    public static MinMaxBounds deserialize(JsonElement element) {
        if (element == null || element.isJsonNull()) return UNBOUNDED;
        if (JsonUtils.isNumber(element)) {
            float f = JsonUtils.getFloat(element, "value");
            return new MinMaxBounds(f, f);
        }
        JsonObject json = JsonUtils.getJsonObject(element, "value");
        Float lo = json.has("min") ? JsonUtils.getFloat(json, "min") : null;
        Float hi = json.has("max") ? JsonUtils.getFloat(json, "max") : null;
        return new MinMaxBounds(lo, hi);
    }
}
