package org.kylepitcock.legacyconnector.v1_7_10.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapedOreRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Forge's {@code ShapedOreRecipe(result, Boolean, Object[] recipe)}: Railcraft passes the flag and the whole recipe as one array. */
class ShapedOreRecipeNestedArgsTest {
    @BeforeAll
    static void boot() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void flagFollowedByOneArrayIsTheRecipe() {
        var recipe = new ShapedOreRecipe(new ItemStack(Items.diamond), Boolean.FALSE,
                new Object[] {" I", " S", 'I', new ItemStack(Items.coal), 'S', new ItemStack(Items.stick)});
        assertFalse(recipe.mcc$mirrored());
        Object[] input = recipe.getInput();
        assertEquals(4, input.length);
        assertEquals(null, input[0]);
        assertNotNull(input[1]);
        assertNotNull(input[3]);
    }

    @Test
    void flatFlagAndRowsStillWork() {
        var recipe = new ShapedOreRecipe(new ItemStack(Items.diamond), Boolean.FALSE, " I", " S", 'I', new ItemStack(Items.coal), 'S', new ItemStack(Items.stick));
        assertEquals(4, recipe.getInput().length);
        assertNotNull(recipe.getInput()[1]);
    }
}
