package org.kylepitcock.legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.RecipeSerializerCompat;

class RecipeSerializerCompatTest {
    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static final String SHAPED = "{\"type\":\"minecraft:crafting_shaped\",\"group\":\"tools\",\"pattern\":[\"XX\",\" S\",\" S\"],"
            + "\"key\":{\"X\":{\"item\":\"minecraft:stone\"},\"S\":{\"item\":\"minecraft:stick\"}},\"result\":{\"item\":\"minecraft:stone_pickaxe\",\"count\":1}}";

    @Test
    void shapedRecipeRoundTripsThroughJsonAndNetwork() {
        var serializer = new ShapedRecipe.Serializer();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "pick");
        ShapedRecipe recipe = RecipeSerializerCompat.shapedFromJson(serializer, id, JsonParser.parseString(SHAPED).getAsJsonObject());
        assertEquals("tools", recipe.getGroup());
        assertEquals(2, recipe.getWidth());
        assertEquals(3, recipe.getHeight());
        assertEquals(Items.STONE_PICKAXE, recipe.getResultItem(RegistryAccess.EMPTY).getItem());

        // the stream codec needs a synced registry access (a running game); the data form is checked by encoding the recipe again
        JsonObject again = serializer.codec().codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, recipe).getOrThrow().getAsJsonObject();
        ShapedRecipe back = RecipeSerializerCompat.shapedFromJson(serializer, id, again);
        assertEquals(recipe.getGroup(), back.getGroup());
        assertEquals(recipe.getWidth(), back.getWidth());
        assertEquals(recipe.getHeight(), back.getHeight());
        assertEquals(recipe.getIngredients().size(), back.getIngredients().size());
    }

    @Test
    void patternHelpersFollowTheLegacyRules() {
        JsonArray rows = JsonParser.parseString("[\" X \",\"   \"]").getAsJsonArray();
        String[] pattern = RecipeSerializerCompat.patternFromJson(rows);
        assertArrayEquals(new String[] {"X"}, RecipeSerializerCompat.shrink(pattern));
        JsonObject key = JsonParser.parseString("{\"X\":{\"item\":\"minecraft:stone\"}}").getAsJsonObject();
        var keys = RecipeSerializerCompat.keyFromJson(key);
        assertEquals(1, RecipeSerializerCompat.dissolvePattern(new String[] {"X"}, keys, 1, 1).size());
        assertThrows(JsonSyntaxException.class, () -> RecipeSerializerCompat.dissolvePattern(new String[] {"Y"}, keys, 1, 1));
        assertEquals(Items.STONE, RecipeSerializerCompat.itemFromJson(JsonParser.parseString("{\"item\":\"minecraft:stone\"}").getAsJsonObject()));
    }
}
