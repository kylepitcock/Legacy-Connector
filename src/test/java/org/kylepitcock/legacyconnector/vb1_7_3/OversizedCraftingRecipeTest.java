package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.recipe.OversizedCraftingRecipe;

class OversizedCraftingRecipeTest {
    @BeforeAll static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true); dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
        // Offline bootstrap omits NeoForge's vanilla registry synchronization setup.
        var sync = net.neoforged.neoforge.registries.BaseMappedRegistry.class.getDeclaredMethod("setSync", boolean.class);
        sync.setAccessible(true);
        sync.invoke(BuiltInRegistries.ITEM, true);
        sync.invoke(BuiltInRegistries.DATA_COMPONENT_TYPE, true);
    }

    @Test void shapedStrictFailureAndLosslessCodecs() {
        var json = JsonParser.parseString("""
                {"category":"misc","pattern":["X"],"key":{"X":{"item":"minecraft:stone"}},
                 "result":{"id":"minecraft:wooden_sword","count":4}}
                """).getAsJsonObject();
        assertTrue(RecipeSerializer.SHAPED_RECIPE.codec().codec().parse(JsonOps.INSTANCE, json).error().isPresent());
        json.getAsJsonObject("result").remove("count");
        var base = RecipeSerializer.SHAPED_RECIPE.codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        var recipe = new OversizedCraftingRecipe.Shaped(base, 4);
        var input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.STONE)));
        assertTrue(recipe.matches(input, null));
        assertFalse(recipe.matches(CraftingInput.of(1, 1, List.of(new ItemStack(Items.DIRT))), null));
        var encoded = OversizedCraftingRecipe.Shaped.CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe).getOrThrow();
        var decoded = OversizedCraftingRecipe.Shaped.CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(4, decoded.assemble(input, null).getCount());
        assertEquals(1, decoded.getResultItem(null).getMaxStackSize());
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        try {
            OversizedCraftingRecipe.Shaped.STREAM.encode(buf, recipe);
            assertEquals(4, OversizedCraftingRecipe.Shaped.STREAM.decode(buf).getResultItem(null).getCount());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.getResultItem(null));
            var stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            assertEquals(4, stack.getCount()); assertEquals(1, stack.getMaxStackSize());
        } finally { buf.release(); }
        encoded.getAsJsonObject().addProperty("count", 0);
        assertTrue(OversizedCraftingRecipe.Shaped.CODEC.codec().parse(JsonOps.INSTANCE, encoded).error().isPresent());
    }

    @Test void shapelessPreservesContainerRemainderAndOutputCopies() {
        var ingredients = net.minecraft.core.NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.MILK_BUCKET));
        var base = new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.WOODEN_SWORD), ingredients);
        var recipe = new OversizedCraftingRecipe.Shapeless(base, 4);
        var input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.MILK_BUCKET)));
        assertTrue(recipe.matches(input, null));
        assertTrue(recipe.getRemainingItems(input).getFirst().is(Items.BUCKET));
        recipe.assemble(input, null).shrink(1);
        assertEquals(4, recipe.assemble(input, null).getCount());
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        try {
            OversizedCraftingRecipe.Shapeless.STREAM.encode(buf, recipe);
            var decoded = OversizedCraftingRecipe.Shapeless.STREAM.decode(buf);
            assertEquals(4, decoded.getResultItem(null).getCount());
            assertTrue(decoded.getRemainingItems(input).getFirst().is(Items.BUCKET));
        } finally { buf.release(); }
    }
}
