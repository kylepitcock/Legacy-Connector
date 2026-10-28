package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** An enchantment condition: an enchantment (any when null) at a level in range. */
public class EnchantmentPredicate {
    public static final EnchantmentPredicate ANY = new EnchantmentPredicate();
    private final Enchantment enchantment;
    private final MinMaxBounds levels;

    public EnchantmentPredicate() {
        this(null, MinMaxBounds.UNBOUNDED);
    }

    public EnchantmentPredicate(Enchantment enchantment, MinMaxBounds levels) {
        this.enchantment = enchantment;
        this.levels = levels;
    }

    public boolean test(Map<Enchantment, Integer> enchantments) {
        if (enchantment != null) {
            Integer level = enchantments.get(enchantment);
            return level != null && (levels == MinMaxBounds.UNBOUNDED || levels.test(level));
        }
        if (levels != MinMaxBounds.UNBOUNDED) {
            for (Integer level : enchantments.values()) if (levels.test(level)) return true;
            return false;
        }
        return true;
    }

    public static EnchantmentPredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        JsonObject o = json.getAsJsonObject();
        Enchantment e = null;
        if (o.has("enchantment")) {
            ResourceLocation id = new ResourceLocation(o.get("enchantment").getAsString());
            e = Enchantment.REGISTRY.getObject(id);
            if (e == null) throw new com.google.gson.JsonSyntaxException("Unknown enchantment '" + id + "'");
        }
        return new EnchantmentPredicate(e, MinMaxBounds.deserialize(o.get("levels")));
    }

    public static EnchantmentPredicate[] deserializeArray(JsonElement json) {
        if (json == null || json.isJsonNull()) return new EnchantmentPredicate[0];
        JsonArray a = json.getAsJsonArray();
        EnchantmentPredicate[] out = new EnchantmentPredicate[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = deserialize(a.get(i));
        return out;
    }
}
