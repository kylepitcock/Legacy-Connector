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
    // Forge's field names: libraries read the maps reflectively (SuperMartijn642's Core Lib asks for "conditions"); the same map objects
    private static final Map<ResourceLocation, IIngredientSerializer<?>> ingredients = INGREDIENTS;
    private static final Map<ResourceLocation, IConditionSerializer<?>> conditions = CONDITIONS;

    private CraftingHelper() {
    }

    public static <T extends net.minecraft.world.item.crafting.Ingredient> IIngredientSerializer<T> register(ResourceLocation key, IIngredientSerializer<T> serializer) {
        INGREDIENTS.put(key, serializer);
        registerType(key, typeFor(serializer));
        return serializer;
    }

    // ---- custom ingredient types (AbstractIngredient subclasses) ----------------------------------------------------------------

    private static final Map<IIngredientSerializer<?>, net.neoforged.neoforge.common.crafting.IngredientType<AbstractIngredient.Bridge>> TYPES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, net.neoforged.neoforge.common.crafting.IngredientType<?>> PENDING_TYPES = new ConcurrentHashMap<>();

    /** The NeoForge ingredient type of a 1.20.1 serializer: its JSON and network forms, read and written by the serializer. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static net.neoforged.neoforge.common.crafting.IngredientType<AbstractIngredient.Bridge> typeFor(IIngredientSerializer<?> serializer) {
        return TYPES.computeIfAbsent(serializer, s -> {
            com.mojang.serialization.MapCodec<AbstractIngredient.Bridge> codec = new com.mojang.serialization.MapCodec<>() {
                @Override
                public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
                    return java.util.stream.Stream.empty();
                }

                @Override
                public <T> com.mojang.serialization.DataResult<AbstractIngredient.Bridge> decode(com.mojang.serialization.DynamicOps<T> ops,
                        com.mojang.serialization.MapLike<T> input) {
                    try {
                        JsonObject obj = new JsonObject();
                        input.entries().forEach(p -> {
                            String k = ops.getStringValue(p.getFirst()).result().orElse(null);
                            if (k != null) obj.add(k, ops.convertTo(JsonOps.INSTANCE, p.getSecond()));
                        });
                        Object parsed = s.parse(obj);
                        if (parsed instanceof AbstractIngredient a) return com.mojang.serialization.DataResult.success((AbstractIngredient.Bridge) a.getCustomIngredient());
                        if (parsed instanceof net.minecraft.world.item.crafting.Ingredient i) return com.mojang.serialization.DataResult.success(new AbstractIngredient.Bridge(i, s));
                        return com.mojang.serialization.DataResult.error(() -> "The ingredient serializer did not return an ingredient");
                    } catch (RuntimeException e) {
                        return com.mojang.serialization.DataResult.error(() -> String.valueOf(e.getMessage()));
                    }
                }

                @Override
                public <T> com.mojang.serialization.RecordBuilder<T> encode(AbstractIngredient.Bridge input, com.mojang.serialization.DynamicOps<T> ops,
                        com.mojang.serialization.RecordBuilder<T> prefix) {
                    JsonElement json = input.toJson();
                    if (json instanceof JsonObject obj) {
                        for (var e : obj.entrySet()) {
                            if (!e.getKey().equals("type")) prefix.add(e.getKey(), JsonOps.INSTANCE.convertTo(ops, e.getValue()));
                        }
                    }
                    return prefix;
                }
            };
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, AbstractIngredient.Bridge> stream = new net.minecraft.network.codec.StreamCodec<>() {
                @Override
                public AbstractIngredient.Bridge decode(net.minecraft.network.RegistryFriendlyByteBuf buf) {
                    Object parsed = ((IIngredientSerializer) s).parse(buf);
                    return parsed instanceof AbstractIngredient a ? (AbstractIngredient.Bridge) a.getCustomIngredient()
                            : new AbstractIngredient.Bridge((net.minecraft.world.item.crafting.Ingredient) parsed, s);
                }

                @Override
                public void encode(net.minecraft.network.RegistryFriendlyByteBuf buf, AbstractIngredient.Bridge value) {
                    ((IIngredientSerializer) s).write(buf, value.owner());
                }
            };
            return new net.neoforged.neoforge.common.crafting.IngredientType<>(codec, stream);
        });
    }

    private static void registerType(ResourceLocation key, net.neoforged.neoforge.common.crafting.IngredientType<?> type) {
        try {
            net.minecraft.core.Registry.register(net.neoforged.neoforge.registries.NeoForgeRegistries.INGREDIENT_TYPES, key, type);
        } catch (RuntimeException e) {
            PENDING_TYPES.put(key, type); // the registry is not open yet (or any more); the register event of the layer retries
        }
    }

    /** Called when NeoForge opens the ingredient type registry: types a mod registered earlier than that. */
    public static void flushTypes(java.util.function.BiConsumer<ResourceLocation, net.neoforged.neoforge.common.crafting.IngredientType<?>> sink) {
        PENDING_TYPES.forEach(sink);
        PENDING_TYPES.clear();
    }

    public static ResourceLocation getID(IIngredientSerializer<?> serializer) {
        for (var e : INGREDIENTS.entrySet()) if (e.getValue() == serializer) return e.getKey();
        return null;
    }

    private static final Map<ResourceLocation, com.mojang.serialization.MapCodec<? extends ICondition>> PENDING_CONDITIONS = new ConcurrentHashMap<>();

    public static <T extends ICondition> IConditionSerializer<T> register(IConditionSerializer<T> serializer) {
        CONDITIONS.put(serializer.getID(), serializer);
        com.mojang.serialization.MapCodec<? extends ICondition> codec = conditionCodec(serializer);
        try {
            org.kylepitcock.legacyconnector.v1_20_1.migrate.MiscCompat.registerLate((net.minecraft.core.Registry) net.neoforged.neoforge.registries.NeoForgeRegistries.CONDITION_SERIALIZERS, serializer.getID(), (Object) codec);
        } catch (RuntimeException e) {
            PENDING_CONDITIONS.put(serializer.getID(), codec); // the registry is not open yet; the layer's register event retries
        }
        return serializer;
    }

    /** Called when NeoForge opens the condition serializer registry: serializers a mod registered earlier than that. */
    public static void flushConditions(java.util.function.BiConsumer<ResourceLocation, com.mojang.serialization.MapCodec<? extends ICondition>> sink) {
        PENDING_CONDITIONS.forEach(sink);
        PENDING_CONDITIONS.clear();
    }

    /** The MapCodec of a 1.20.1 condition serializer: the JSON members of the condition object go through {@code read}/{@code write}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static com.mojang.serialization.MapCodec<? extends ICondition> conditionCodec(IConditionSerializer serializer) {
        return new com.mojang.serialization.MapCodec<ICondition>() {
            @Override
            public <U> java.util.stream.Stream<U> keys(com.mojang.serialization.DynamicOps<U> ops) {
                return java.util.stream.Stream.empty();
            }

            @Override
            public <U> com.mojang.serialization.DataResult<ICondition> decode(com.mojang.serialization.DynamicOps<U> ops, com.mojang.serialization.MapLike<U> input) {
                try {
                    JsonObject json = new JsonObject();
                    input.entries().forEach(p -> {
                        String key = ops.getStringValue(p.getFirst()).result().orElse(null);
                        if (key != null) json.add(key, ops.convertTo(JsonOps.INSTANCE, p.getSecond()));
                    });
                    return com.mojang.serialization.DataResult.success((ICondition) serializer.read(json));
                } catch (RuntimeException e) {
                    return com.mojang.serialization.DataResult.error(() -> "Cannot read condition " + serializer.getID() + ": " + e);
                }
            }

            @Override
            public <U> com.mojang.serialization.RecordBuilder<U> encode(ICondition input, com.mojang.serialization.DynamicOps<U> ops, com.mojang.serialization.RecordBuilder<U> prefix) {
                try {
                    JsonObject json = new JsonObject();
                    serializer.write(json, input);
                    for (var e : json.entrySet()) if (!e.getKey().equals("type")) prefix.add(e.getKey(), JsonOps.INSTANCE.convertTo(ops, e.getValue()));
                } catch (RuntimeException e) {
                    return prefix.withErrorsFrom(com.mojang.serialization.DataResult.error(() -> "Cannot write condition " + serializer.getID() + ": " + e));
                }
                return prefix;
            }
        };
    }

    /** {@code codec()} of a 1.20.1 {@code ICondition} implementation: the codec of the serializer registered under its id. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static com.mojang.serialization.MapCodec conditionCodecOf(ResourceLocation id) {
        IConditionSerializer s = CONDITIONS.get(id);
        if (s == null) throw new IllegalStateException("No condition serializer registered for " + id);
        return conditionCodec(s);
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

    /** A condition from its JSON object ({"type": ...}). */
    public static ICondition getCondition(JsonObject json) {
        return ICondition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new);
    }

    public static boolean processConditions(com.google.gson.JsonArray conditions, ICondition.IContext context) {
        for (JsonElement e : conditions) {
            if (!getCondition(e.getAsJsonObject()).test(context)) return false;
        }
        return true;
    }

    public static com.google.gson.JsonArray serialize(ICondition[] conditions) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (ICondition c : conditions) array.add(serialize(c));
        return array;
    }

    /** An ingredient from JSON (an item, a tag or an array of them); {@code array} said whether an array was allowed. */
    public static net.minecraft.world.item.crafting.Ingredient getIngredient(JsonElement json, boolean array) {
        if (json == null || json.isJsonNull()) throw new JsonSyntaxException("Json cannot be null");
        return net.minecraft.world.item.crafting.Ingredient.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new);
    }
}
