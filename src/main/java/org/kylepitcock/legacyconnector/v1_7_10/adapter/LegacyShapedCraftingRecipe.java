package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge;

/**
 * A shaped legacy recipe that is a SUBCLASS of Forge's ShapedOreRecipe / vanilla ShapedRecipes with its own matching or result logic
 * (Thermal Expansion's machine and machine-upgrade recipes write the machine level and augments into the result's NBT). The pattern is
 * the data pack's, so recipe viewers show it as a normal shaped recipe; matching and the result come from the legacy object.
 */
public final class LegacyShapedCraftingRecipe extends ShapedRecipe {
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LegacyShapedCraftingRecipe>> SERIALIZER =
            LegacyCraftingRecipe.REGISTER.register("legacy_shaped_recipe", Serializer::new);

    private final int index;
    private final ShapedRecipePattern pattern;
    private final ItemStack base;

    public LegacyShapedCraftingRecipe(ShapedRecipePattern pattern, ItemStack base, int index) {
        super("", CraftingBookCategory.MISC, pattern, base, false);
        this.pattern = pattern;
        this.base = base;
        this.index = index;
    }

    private IRecipe legacy() {
        return RecipeBridge.custom(index);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        IRecipe r = legacy();
        if (r == null || input.isEmpty()) return false;
        try {
            return r.matches(LegacyCraftingRecipe.view(input), World.mcc$wrap(level));
        } catch (RuntimeException | LinkageError e) {
            LegacyCraftingRecipe.failed(e);
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        IRecipe r = legacy();
        if (r == null) return ItemStack.EMPTY;
        try {
            var out = r.getCraftingResult(LegacyCraftingRecipe.view(input));
            return out == null ? ItemStack.EMPTY : legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(out);
        } catch (RuntimeException | LinkageError e) {
            LegacyCraftingRecipe.failed(e);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return base;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    private static final class Serializer implements RecipeSerializer<LegacyShapedCraftingRecipe> {
        private static final MapCodec<LegacyShapedCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.base),
                Codec.INT.fieldOf("index").forGetter(r -> r.index)).apply(i, LegacyShapedCraftingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, LegacyShapedCraftingRecipe> STREAM = StreamCodec.composite(
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern, ItemStack.STREAM_CODEC, r -> r.base, ByteBufCodecs.VAR_INT, r -> r.index,
                LegacyShapedCraftingRecipe::new);

        @Override
        public MapCodec<LegacyShapedCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LegacyShapedCraftingRecipe> streamCodec() {
            return STREAM;
        }
    }
}
