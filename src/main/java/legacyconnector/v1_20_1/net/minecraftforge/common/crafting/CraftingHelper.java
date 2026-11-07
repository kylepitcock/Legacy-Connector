package legacyconnector.v1_20_1.net.minecraftforge.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_20_1.net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.ItemStackNbt;

/**
 * Forge 1.20.1's recipe JSON helpers, over NeoForge 1.21.1 (which reads recipes through codecs). A mod's own recipe type keeps its 1.20.1 JSON format
 * (the data converter leaves it alone) and its serializer parses it with these.
 */
public final class CraftingHelper {
    private static final Map<ResourceLocation, IIngredientSerializer<?>> INGREDIENTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, IConditionSerializer<?>> CONDITIONS = new ConcurrentHashMap<>();

    private CraftingHelper() {
    }

    public static <T extends net.minecraft.world.item.crafting.Ingredient> IIngredientSerializer<T> register(ResourceLocation key, IIngredientSerializer<T> serializer) {
        INGREDIENTS.put(key, serializer);
        return serializer;
    }

    public static ResourceLocation getID(IIngredientSerializer<?> serializer) {
        for (var e : INGREDIENTS.entrySet()) if (e.getValue() == serializer) return e.getKey();
        return null;
    }

    public static <T extends ICondition> IConditionSerializer<T> register(IConditionSerializer<T> serializer) {
        CONDITIONS.put(serializer.getID(), serializer);
        return serializer;
    }

    /** A 1.20.1 item stack object: {@code {"item": "ns:id", "count": 2, "nbt": {..} or "{snbt}"}}. */
    public static ItemStack getItemStack(JsonObject json, boolean readNBT) {
        return getItemStack(json, readNBT, false);
    }

    public static ItemStack getItemStack(JsonObject json, boolean readNBT, boolean disallowsAirInRecipe) {
        String itemName = net.minecraft.util.GsonHelper.getAsString(json, "item");
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName)).orElseThrow(() -> new JsonSyntaxException("Unknown item '" + itemName + "'"));
        if (disallowsAirInRecipe && item == Items.AIR) throw new JsonSyntaxException("Invalid item: " + itemName);
        ItemStack stack = new ItemStack(item, net.minecraft.util.GsonHelper.getAsInt(json, "count", 1));
        if (readNBT && json.has("nbt")) ItemStackNbt.setTag(stack, getNBT(json.get("nbt")));
        return stack;
    }

    /** NBT given as an SNBT string or as a JSON object. */
    public static CompoundTag getNBT(JsonElement element) {
        try {
            if (element.isJsonObject()) return (CompoundTag) JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element);
            return TagParser.parseTag(net.minecraft.util.GsonHelper.convertToString(element, "nbt"));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new JsonSyntaxException("Invalid NBT Entry: " + e);
        }
    }

    /** 1.20.1 recipes carried {@code conditions}; the data converter writes them as {@code neoforge:conditions}, which this evaluates. */
    public static boolean processConditions(JsonObject json, String memberName, ICondition.IContext context) {
        String member = json.has(memberName) ? memberName : json.has("neoforge:conditions") ? "neoforge:conditions" : null;
        if (member == null) return true;
        return ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json.get(member)).result().map(list -> list.stream().allMatch(c -> c.test(context))).orElse(true);
    }

    public static JsonObject serialize(ICondition condition) {
        return ICondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).result().map(JsonElement::getAsJsonObject).orElseGet(JsonObject::new);
    }
}
