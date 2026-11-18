package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;

/** NEI 1.4.7 / 1.5.2 recipe handler API: the screen fields of the (shim) recipe screen and the ArrayList-returning handler methods. */
class NeiForwardTest {
    /** A 1.4.7 handler reads the recipe screen's width, xSize, height, ySize as notch fields of GuiRecipe; they are GuiScreen's and GuiContainer's. */
    @Test
    void recipeScreenFieldsResolveThroughGuiContainer() {
        NotchNames n = NotchNames.get("1.4.7");
        Map<String, String[]> none = Map.of();
        assertEquals("field_74194_b", n.field("codechicken/nei/recipe/GuiRecipe", "b", none), "xSize");
        assertEquals("field_74195_c", n.field("codechicken/nei/recipe/GuiRecipe", "c", none), "ySize");
        assertNotNull(n.field("codechicken/nei/recipe/GuiRecipe", "g", none), "width");
        assertNotNull(n.field("codechicken/nei/recipe/GuiRecipe", "h", none), "height");
        assertEquals("field_74194_b", n.field("codechicken/nei/recipe/GuiCraftingRecipe", "b", none));
        // not a NEI screen: unknown owners stay unresolved
        assertEquals(null, n.field("codechicken/nei/recipe/GuiSomethingElse", "b", none));
        NotchNames n152 = NotchNames.get("1.5.2");
        assertEquals("field_74194_b", n152.field("codechicken/nei/recipe/GuiRecipe", "b", none));
    }

    @Test
    void arrayListShapedHandlerMethodsAreBridged() throws Exception {
        List<String> rows;
        try (InputStream in = NeiForwardTest.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.5.2/bridge-methods.tsv")) {
            rows = List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\r?\n"));
        }
        for (String row : List.of(
                "codechicken/nei/recipe/IRecipeHandler\tgetIngredientStacks\t(I)Ljava/util/ArrayList;\tgetIngredientStacks\t(I)Ljava/util/List;",
                "codechicken/nei/recipe/IRecipeHandler\tgetOtherStacks\t(I)Ljava/util/ArrayList;\tgetOtherStacks\t(I)Ljava/util/List;",
                "codechicken/nei/recipe/TemplateRecipeHandler$CachedRecipe\tgetIngredients\t()Ljava/util/ArrayList;\tgetIngredients\t()Ljava/util/List;",
                "codechicken/nei/recipe/TemplateRecipeHandler$CachedRecipe\tgetOtherStacks\t()Ljava/util/ArrayList;\tgetOtherStacks\t()Ljava/util/List;",
                "codechicken/nei/recipe/TemplateRecipeHandler$CachedRecipe\tgetCycledIngredients\t(ILjava/util/ArrayList;)Ljava/util/ArrayList;\tgetCycledIngredients\t(ILjava/util/List;)Ljava/util/List;")) {
            assertTrue(rows.contains(row), "no bridge row " + row);
        }
    }
}
