package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A number range of an advancement condition (either end open); a plain number in JSON is an exact value. */
public class MinMaxBounds {
    public static final MinMaxBounds UNBOUNDED = new MinMaxBounds(null, null);
    private final Float min;
    private final Float max;

    public MinMaxBounds(Float min, Float max) {
        this.min = min;
        this.max = max;
    }

    public boolean test(float value) {
        return (min == null || min <= value) && (max == null || max >= value);
    }

    public boolean testSquare(double value) {
        return (min == null || (double) (min * min) <= value) && (max == null || (double) (max * max) >= value);
    }

    public static MinMaxBounds deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return UNBOUNDED;
        if (json.isJsonPrimitive()) {
            float f = json.getAsFloat();
            return new MinMaxBounds(f, f);
        }
        JsonObject o = json.getAsJsonObject();
        Float min = o.has("min") ? o.get("min").getAsFloat() : null;
        Float max = o.has("max") ? o.get("max").getAsFloat() : null;
        return new MinMaxBounds(min, max);
    }
}
