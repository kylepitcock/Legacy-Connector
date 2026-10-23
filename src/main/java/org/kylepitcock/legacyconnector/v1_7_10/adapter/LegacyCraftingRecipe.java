package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.v1_7_10.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.RecipeBridge;

/**
 * A crafting recipe that is a custom legacy {@link IRecipe} (IC2's AdvRecipe, AE2's recipes, Blood Orb recipes, ...): the
 * data pack only names it by index, matching and the result come from the legacy object, which sees a view of the grid.
 */
public final class LegacyCraftingRecipe implements CraftingRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> REGISTER = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "legacyconnector");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LegacyCraftingRecipe>> SERIALIZER = REGISTER.register("legacy_recipe", Serializer::new);
    private static final AtomicInteger FAILURES = new AtomicInteger();

    private final int index;

    public LegacyCraftingRecipe(int index) {
        this.index = index;
    }

    private IRecipe legacy() {
        return RecipeBridge.custom(index);
    }

    private static InventoryCrafting view(CraftingInput input) {
        var menu = new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        NonNullList<ItemStack> items = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) items.set(i, input.getItem(i));
        return new InventoryCrafting(new TransientCraftingContainer(menu, input.width(), input.height(), items));
    }

    private static void failed(Throwable e) {
        if (FAILURES.incrementAndGet() <= 5) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Recipes").warn("A custom legacy recipe failed: {}", e.toString());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        IRecipe r = legacy();
        if (r == null) return false;
        try {
            return r.matches(view(input), World.mcc$wrap(level));
        } catch (RuntimeException | LinkageError e) {
            failed(e);
            return false;
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        IRecipe r = legacy();
        if (r == null) return ItemStack.EMPTY;
        try {
            var out = r.getCraftingResult(view(input));
            return out == null ? ItemStack.EMPTY : legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(out);
        } catch (RuntimeException | LinkageError e) {
            failed(e);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        IRecipe r = legacy();
        try {
            var out = r == null ? null : r.getRecipeOutput();
            return out == null ? ItemStack.EMPTY : legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(out);
        } catch (RuntimeException | LinkageError e) {
            failed(e);
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean isSpecial() {
        return true; // no ingredients to list: keep it out of the recipe book
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    private static final class Serializer implements RecipeSerializer<LegacyCraftingRecipe> {
        private static final MapCodec<LegacyCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("index").forGetter(r -> r.index)).apply(i, LegacyCraftingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, LegacyCraftingRecipe> STREAM = StreamCodec.composite(ByteBufCodecs.VAR_INT, r -> r.index,
                LegacyCraftingRecipe::new);

        @Override
        public MapCodec<LegacyCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LegacyCraftingRecipe> streamCodec() {
            return STREAM;
        }
    }
}
