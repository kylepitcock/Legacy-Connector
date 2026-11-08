package org.kylepitcock.legacyconnector.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Legacy crafting may produce more items than one inventory slot can hold. Keep native crafting
 * and inventory handling, carrying the output count separately from the strict data-pack stack codec. */
public final class OversizedCraftingRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTER = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "legacyconnector");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Shaped>> SHAPED = REGISTER.register("oversized_shaped", () -> serializer(Shaped.CODEC, Shaped.STREAM));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Shapeless>> SHAPELESS = REGISTER.register("oversized_shapeless", () -> serializer(Shapeless.CODEC, Shapeless.STREAM));
    private OversizedCraftingRecipe() {}

    private static int positive(int count) {
        if (count < 1) throw new IllegalArgumentException("Crafting output count must be positive");
        return count;
    }

    private static <T extends CraftingRecipe> RecipeSerializer<T> serializer(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> stream) {
        return new RecipeSerializer<>() {
            public MapCodec<T> codec() { return codec; }
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() { return stream; }
        };
    }

    public static final class Shaped extends ShapedRecipe {
        public static final MapCodec<Shaped> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RecipeSerializer.SHAPED_RECIPE.codec().codec().fieldOf("recipe").forGetter((Shaped r) -> r.delegate),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(r -> r.count)).apply(i, Shaped::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Shaped> STREAM = StreamCodec.composite(
                RecipeSerializer.SHAPED_RECIPE.streamCodec(), r -> r.delegate,
                ByteBufCodecs.VAR_INT, r -> r.count, Shaped::new);
        private final ShapedRecipe delegate;
        private final int count;
        public Shaped(ShapedRecipe delegate, int count) {
            super(delegate.getGroup(), delegate.category(), delegate.pattern,
                    delegate.getResultItem(null).copyWithCount(positive(count)), delegate.showNotification());
            this.delegate = delegate;
            this.count = count;
        }
        @Override public RecipeSerializer<?> getSerializer() { return SHAPED.get(); }
    }

    public static final class Shapeless extends ShapelessRecipe {
        public static final MapCodec<Shapeless> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RecipeSerializer.SHAPELESS_RECIPE.codec().codec().fieldOf("recipe").forGetter((Shapeless r) -> r.delegate),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(r -> r.count)).apply(i, Shapeless::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Shapeless> STREAM = StreamCodec.composite(
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec(), r -> r.delegate,
                ByteBufCodecs.VAR_INT, r -> r.count, Shapeless::new);
        private final ShapelessRecipe delegate;
        private final int count;
        public Shapeless(ShapelessRecipe delegate, int count) {
            super(delegate.getGroup(), delegate.category(), delegate.getResultItem(null).copyWithCount(positive(count)), delegate.getIngredients());
            this.delegate = delegate;
            this.count = count;
        }
        @Override public RecipeSerializer<?> getSerializer() { return SHAPELESS.get(); }
    }
}
