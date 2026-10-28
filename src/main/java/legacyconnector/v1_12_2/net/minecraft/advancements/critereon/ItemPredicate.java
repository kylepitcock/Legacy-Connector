package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.potion.PotionType;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** An item condition of advancements and loot: item, metadata, count, remaining durability, enchantments, potion and NBT. */
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
        if (stack == null) return this == ANY;
        if (item != null && stack.getItem() != item) return false;
        if (data != null && stack.getMetadata() != data) return false;
        if (!count.test(stack.getCount())) return false;
        if (durability != MinMaxBounds.UNBOUNDED && !stack.isItemStackDamageable()) return false;
        if (!durability.test(stack.getMaxDamage() - stack.getItemDamage())) return false;
        if (!nbt.test(stack)) return false;
        if (enchantments.length > 0) {
            Map<Enchantment, Integer> map = legacyconnector.v1_12_2.net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack);
            for (EnchantmentPredicate p : enchantments) if (!p.test(map)) return false;
        }
        return potion == null || potion == legacyconnector.v1_12_2.net.minecraft.potion.PotionUtils.getPotionFromItem(stack);
    }

    public static ItemPredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        JsonObject o = json.getAsJsonObject();
        MinMaxBounds count = MinMaxBounds.deserialize(o.get("count"));
        MinMaxBounds durability = MinMaxBounds.deserialize(o.get("durability"));
        Integer data = o.has("data") ? o.get("data").getAsInt() : null;
        NBTPredicate nbt = NBTPredicate.deserialize(o.get("nbt"));
        Item item = null;
        if (o.has("item")) {
            ResourceLocation id = new ResourceLocation(o.get("item").getAsString());
            item = Item.REGISTRY.getObject(id);
            if (item == null) throw new com.google.gson.JsonSyntaxException("Unknown item id '" + id + "'");
        }
        EnchantmentPredicate[] enchantments = EnchantmentPredicate.deserializeArray(o.get("enchantments"));
        PotionType potion = null;
        if (o.has("potion")) {
            ResourceLocation id = new ResourceLocation(o.get("potion").getAsString());
            potion = PotionType.REGISTRY.getObject(id);
        }
        return new ItemPredicate(item, data, count, durability, enchantments, potion, nbt);
    }

    public static ItemPredicate[] deserializeArray(JsonElement json) {
        if (json == null || json.isJsonNull()) return new ItemPredicate[0];
        JsonArray a = json.getAsJsonArray();
        ItemPredicate[] out = new ItemPredicate[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = deserialize(a.get(i));
        return out;
    }
}
