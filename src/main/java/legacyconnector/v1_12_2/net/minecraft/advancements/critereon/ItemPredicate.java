package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionType;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils;
import legacyconnector.v1_12_2.net.minecraft.util.JsonUtils;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * Which stacks a criterion accepts: item, metadata, count, durability, enchantments, potion and tag, as 1.12.2 read them from
 * JSON. A {@code "type"} names a predicate a mod registered with Forge's {@code ItemPredicates}.
 */
public class ItemPredicate {
    public static final ItemPredicate ANY = new ItemPredicate();
    private final Item item;
    private final Integer data;
    private final MinMaxBounds count;
    private final MinMaxBounds durability;
    private final EnchantmentPredicate[] enchantments;
    private final PotionType potion;
    private final NBTPredicate nbt;

    public ItemPredicate() {
        this(null, null, MinMaxBounds.UNBOUNDED, MinMaxBounds.UNBOUNDED, new EnchantmentPredicate[0], null, NBTPredicate.ANY);
    }

    public ItemPredicate(Item item, Integer data, MinMaxBounds count, MinMaxBounds durability, EnchantmentPredicate[] enchantments, PotionType potion,
            NBTPredicate nbt) {
        this.item = item;
        this.data = data;
        this.count = count;
        this.durability = durability;
        this.enchantments = enchantments;
        this.potion = potion;
        this.nbt = nbt;
    }

    public boolean test(ItemStack stack) {
        if (item != null && stack.getItem() != item) return false;
        if (data != null && stack.getMetadata() != data) return false;
        if (!count.test(stack.getCount())) return false;
        if (durability != MinMaxBounds.UNBOUNDED && !stack.isItemStackDamageable()) return false;
        if (!durability.test(stack.getMaxDamage() - stack.getItemDamage())) return false;
        if (!nbt.test(stack)) return false;
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        for (EnchantmentPredicate e : enchantments) if (!e.test(map)) return false;
        return potion == null || potion == PotionUtils.getPotionFromItem(stack);
    }

    @SuppressWarnings("unchecked")
    public static ItemPredicate deserialize(JsonElement element) {
        if (element == null || element.isJsonNull()) return ANY;
        JsonObject json = JsonUtils.getJsonObject(element, "item");
        if (json.has("type")) {
            ResourceLocation rl = new ResourceLocation(JsonUtils.getString(json, "type"));
            Object factory = null;
            try {
                factory = org.kylepitcock.legacyconnector.core.synth.Synth.callStatic("net.minecraftforge.advancements.critereon.ItemPredicates.get",
                        new Object[] {rl});
            } catch (Throwable t) {
                // below
            }
            if (factory instanceof java.util.function.Function<?, ?> f) return ((java.util.function.Function<JsonObject, ItemPredicate>) f).apply(json);
            throw new JsonSyntaxException("There is no ItemPredicate of type " + rl);
        }
        MinMaxBounds count = MinMaxBounds.deserialize(json.get("count"));
        MinMaxBounds durability = MinMaxBounds.deserialize(json.get("durability"));
        Integer data = json.has("data") ? JsonUtils.getInt(json, "data") : null;
        NBTPredicate nbt = NBTPredicate.deserialize(json.get("nbt"));
        Item item = null;
        if (json.has("item")) {
            ResourceLocation id = new ResourceLocation(JsonUtils.getString(json, "item"));
            item = Item.REGISTRY.getObject(id);
            if (item == null) throw new JsonSyntaxException("Unknown item id '" + id + "'");
        }
        EnchantmentPredicate[] enchantments = EnchantmentPredicate.deserializeArray(json.get("enchantments"));
        PotionType potion = null;
        if (json.has("potion")) {
            ResourceLocation id = new ResourceLocation(JsonUtils.getString(json, "potion"));
            potion = PotionType.REGISTRY.getObject(id);
            if (potion == null) throw new JsonSyntaxException("Unknown potion '" + id + "'");
        }
        return new ItemPredicate(item, data, count, durability, enchantments, potion, nbt);
    }

    public static ItemPredicate[] deserializeArray(JsonElement element) {
        if (element == null || element.isJsonNull()) return new ItemPredicate[0];
        JsonArray array = JsonUtils.getJsonArray(element, "items");
        ItemPredicate[] out = new ItemPredicate[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = deserialize(array.get(i));
        return out;
    }
}
