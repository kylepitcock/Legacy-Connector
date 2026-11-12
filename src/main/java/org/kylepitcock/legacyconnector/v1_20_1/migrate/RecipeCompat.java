package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Recipe calls of 1.20.1 code: recipes are matched against a {@code Container} there and against a {@code RecipeInput} here, and the recipe manager hands out
 * {@code RecipeHolder}s (a recipe plus its id) instead of recipes. (The overrides of a mod's own recipe classes are bridge methods, see
 * {@code mc-bridges.tsv}; {@link BridgeHelpers} converts the argument.)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class RecipeCompat {
    private RecipeCompat() {}

    public static boolean matches(Recipe recipe, Container container, Level level) {
        return recipe.matches(BridgeHelpers.input(container), level);
    }

    public static ItemStack assemble(Recipe recipe, Container container, RegistryAccess access) {
        MigrateContext.remember(access);
        return recipe.assemble(BridgeHelpers.input(container), access);
    }

    public static ItemStack getResultItem(Recipe recipe, RegistryAccess access) {
        MigrateContext.remember(access);
        return recipe.getResultItem(access);
    }

    public static net.minecraft.core.NonNullList<ItemStack> getRemainingItems(Recipe recipe, Container container) {
        return recipe.getRemainingItems(BridgeHelpers.input(container));
    }

    public static Optional getRecipeFor(RecipeManager manager, RecipeType type, Container container, Level level) {
        Optional<RecipeHolder> found = manager.getRecipeFor(type, BridgeHelpers.input(container), level);
        return found.map(RecipeHolder::value);
    }

    public static List getRecipesFor(RecipeManager manager, RecipeType type, Container container, Level level) {
        List<RecipeHolder> found = manager.getRecipesFor(type, BridgeHelpers.input(container), level);
        return found.stream().map(RecipeHolder::value).toList();
    }

    public static boolean contains(net.minecraft.stats.ServerRecipeBook book, Recipe recipe) {
        if (book == null || recipe == null) return false;
        net.minecraft.resources.ResourceLocation id = getId(recipe);
        return id != null && book.contains(id);
    }

    /** {@code Recipe.getId()}: recipes do not know their id in 1.21.1; it is found through the server's recipe manager (null for a recipe it does not hold). */
    public static net.minecraft.resources.ResourceLocation getId(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (net.minecraft.world.item.crafting.RecipeHolder<?> h : server.getRecipeManager().getRecipes()) if (h.value() == recipe) return h.id();
        return null;
    }

    // ---- recipe serializers: 1.20.1 JSON / FriendlyByteBuf form bridged to the codec pair of 1.21.1 -------------------------------

    private static final net.minecraft.resources.ResourceLocation UNKNOWN_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "unknown");

    private static java.lang.reflect.Method legacy(Object serializer, String name, Class<?>... params) {
        try {
            var m = serializer.getClass().getMethod(name, params);
            return m.getDeclaringClass().isInterface() ? null : m;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Object invoke(java.lang.reflect.Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code RecipeSerializer.fromJson(id, json)} on any serializer: the mod's legacy method, or the codec of a modern serializer. */
    public static Recipe fromJson(net.minecraft.world.item.crafting.RecipeSerializer serializer, net.minecraft.resources.ResourceLocation id, com.google.gson.JsonObject json) {
        var m = legacy(serializer, "fromJson", net.minecraft.resources.ResourceLocation.class, com.google.gson.JsonObject.class);
        if (m != null) return (Recipe) invoke(m, serializer, id, json);
        com.mojang.serialization.DataResult<Object> r = serializer.codec().codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json);
        return (Recipe) r.getOrThrow(msg -> new com.google.gson.JsonParseException(String.valueOf(msg)));
    }

    /** {@code RecipeSerializer.fromNetwork(id, buf)}. */
    public static Recipe fromNetwork(net.minecraft.world.item.crafting.RecipeSerializer serializer, net.minecraft.resources.ResourceLocation id, net.minecraft.network.FriendlyByteBuf buf) {
        var m = legacy(serializer, "fromNetwork", net.minecraft.resources.ResourceLocation.class, net.minecraft.network.FriendlyByteBuf.class);
        if (m != null) return (Recipe) invoke(m, serializer, id, buf);
        return (Recipe) serializer.streamCodec().decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
    }

    /** {@code RecipeSerializer.toNetwork(buf, recipe)}. */
    public static void toNetwork(net.minecraft.world.item.crafting.RecipeSerializer serializer, net.minecraft.network.FriendlyByteBuf buf, Recipe recipe) {
        var m = legacy(serializer, "toNetwork", net.minecraft.network.FriendlyByteBuf.class, Recipe.class);
        if (m != null) {
            invoke(m, serializer, buf, recipe);
            return;
        }
        serializer.streamCodec().encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, recipe);
    }

    /** {@code codec()} of a class that declares the legacy serializer methods (added by the translator): the JSON form read through {@code JsonOps}. */
    public static com.mojang.serialization.MapCodec codec(Object serializer) {
        var m = legacy(serializer, "fromJson", net.minecraft.resources.ResourceLocation.class, com.google.gson.JsonObject.class);
        return new com.mojang.serialization.MapCodec<Recipe>() {
            @Override
            public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
                return java.util.stream.Stream.empty();
            }

            @Override
            public <T> com.mojang.serialization.DataResult<Recipe> decode(com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.MapLike<T> input) {
                if (m == null) return com.mojang.serialization.DataResult.error(() -> "The recipe serializer has no fromJson");
                try {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                    input.entries().forEach(p -> {
                        String k = ops.getStringValue(p.getFirst()).result().orElse(null);
                        if (k != null) obj.add(k, ops.convertTo(com.mojang.serialization.JsonOps.INSTANCE, p.getSecond()));
                    });
                    return com.mojang.serialization.DataResult.success((Recipe) invoke(m, serializer, UNKNOWN_ID, obj));
                } catch (RuntimeException e) {
                    return com.mojang.serialization.DataResult.error(() -> String.valueOf(e.getMessage()));
                }
            }

            @Override
            public <T> com.mojang.serialization.RecordBuilder<T> encode(Recipe input, com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.RecordBuilder<T> prefix) {
                return prefix.withErrorsFrom(com.mojang.serialization.DataResult.error(() -> "Legacy recipe serializers cannot write JSON"));
            }
        };
    }

    /** {@code streamCodec()}: the legacy {@code toNetwork} / {@code fromNetwork} pair over the registry-aware buffer. */
    public static net.minecraft.network.codec.StreamCodec streamCodec(Object serializer) {
        var to = legacy(serializer, "toNetwork", net.minecraft.network.FriendlyByteBuf.class, Recipe.class);
        var from = legacy(serializer, "fromNetwork", net.minecraft.resources.ResourceLocation.class, net.minecraft.network.FriendlyByteBuf.class);
        return new net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Recipe>() {
            @Override
            public Recipe decode(net.minecraft.network.RegistryFriendlyByteBuf buf) {
                return (Recipe) invoke(from, serializer, UNKNOWN_ID, buf);
            }

            @Override
            public void encode(net.minecraft.network.RegistryFriendlyByteBuf buf, Recipe recipe) {
                invoke(to, serializer, buf, recipe);
            }
        };
    }
}
