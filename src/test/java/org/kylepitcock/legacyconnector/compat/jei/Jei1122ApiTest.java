package org.kylepitcock.legacyconnector.compat.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeHandler;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferError;
import legacyconnector.v1_12_2.mezz.jei.collect.ListMultiMap;
import legacyconnector.v1_12_2.mezz.jei.util.ErrorUtil;
import org.junit.jupiter.api.Test;

/** The JEI 1.12 API members the bridge implements (no game needed: nothing here touches an item stack). */
class Jei1122ApiTest {
    record Gizmo(String name) {}

    static final class GizmoWrapper implements IRecipeWrapper {
        final Gizmo gizmo;

        GizmoWrapper(Gizmo gizmo) {
            this.gizmo = gizmo;
        }

        @Override
        public void getIngredients(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients) {
            ingredients.setInput(String.class, gizmo.name());
        }
    }

    static IRecipeHandler<Gizmo> handler(boolean valid) {
        return new IRecipeHandler<>() {
            @Override
            public Class<Gizmo> getRecipeClass() {
                return Gizmo.class;
            }

            @Override
            public String getRecipeCategoryUid(Gizmo recipe) {
                return "mod.gizmo";
            }

            @Override
            public IRecipeWrapper getRecipeWrapper(Gizmo recipe) {
                return new GizmoWrapper(recipe);
            }

            @Override
            public boolean isRecipeValid(Gizmo recipe) {
                return valid;
            }
        };
    }

    @Test
    void jei3HandlersTurnRecipeObjectsIntoWrappersOfTheirCategory() {
        var registry = new Jei1122Bridge.Registry();
        registry.addRecipeHandlers(handler(true));
        registry.addRecipes(List.of(new Gizmo("a"), new Gizmo("b"), "not a recipe"));
        registry.resolvePending();
        var wrappers = registry.recipes.get("mod.gizmo");
        assertEquals(2, wrappers.size());
        assertEquals("a", ((GizmoWrapper) wrappers.get(0)).gizmo.name());
        assertTrue(registry.pendingRecipes.isEmpty());
        assertEquals(2, registry.wrapperOf.size());
    }

    @Test
    void jei3InvalidRecipesAreDropped() {
        var registry = new Jei1122Bridge.Registry();
        registry.addRecipeHandlers(handler(false));
        registry.addRecipes(List.of(new Gizmo("a")));
        registry.resolvePending();
        assertTrue(registry.recipes.isEmpty());
    }

    @Test
    void categoriesAndRecordedOnlyRegistrationsAreKept() {
        var registry = new Jei1122Bridge.Registry();
        registry.addRecipeCategories((legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory<?>) null);
        assertTrue(registry.extraCategories.isEmpty());
        registry.addGhostIngredientHandler(null, null);
        registry.addRecipeRegistryPlugin(null);
        assertTrue(registry.ghosts.isEmpty());
        assertTrue(registry.registryPlugins.isEmpty());
        registry.clear();
    }

    @Test
    void createDrawableWithTextureSizeKeepsTheSizeOfThePart() {
        var plain = (Jei1122Bridge.TextureDrawable) Jei1122Bridge.Helpers.INSTANCE.createDrawable(null, 10, 20, 30, 40, 128, 64);
        assertEquals(30, plain.getWidth());
        assertEquals(40, plain.getHeight());
        assertEquals(128, plain.texWidth());
        assertEquals(64, plain.texHeight());
        var padded = (Jei1122Bridge.TextureDrawable) Jei1122Bridge.Helpers.INSTANCE.createDrawable(null, 0, 0, 30, 40, 1, 2, 3, 4);
        assertEquals(30 + 3 + 4, padded.getWidth());
        assertEquals(40 + 1 + 2, padded.getHeight());
    }

    @Test
    void transferHelperBuildsTheErrorsJei4Had() {
        var helper = Jei1122Bridge.Helpers.INSTANCE.getRecipeTransferHandlerHelper();
        assertSame(helper, Jei1122Bridge.Helpers.INSTANCE.recipeTransferHandlerHelper());
        assertEquals(IRecipeTransferError.Type.INTERNAL, helper.createInternalError().getType());
        var user = (Jei1122Transfer.Error) helper.createUserErrorWithTooltip("missing");
        assertEquals(IRecipeTransferError.Type.USER_FACING, user.getType());
        assertEquals("missing", user.message());
        var slots = (Jei1122Transfer.Error) helper.createUserErrorForSlots("slots", List.of(1, 4));
        assertEquals(List.of(1, 4), List.copyOf(slots.slots()));
    }

    @Test
    void transferRegistrationsAreRecordedPerKind() {
        List<Jei1122Transfer.Entry> into = new ArrayList<>();
        var registry = Jei1122Transfer.registry(into);
        registry.addRecipeTransferHandler(legacyconnector.v1_12_2.net.minecraft.inventory.Container.class, "mod.gizmo", 0, 2, 2, 36);
        registry.addRecipeTransferHandler(new legacyconnector.v1_12_2.mezz.jei.transfer.BasicRecipeTransferInfo<>(
                legacyconnector.v1_12_2.net.minecraft.inventory.Container.class, "mod.other", 1, 1, 2, 36));
        registry.addRecipeTransferHandler((legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandler<?>) null, "x");
        assertEquals(2, into.size());
        assertEquals("mod.gizmo", into.get(0).uid());
        assertEquals("mod.other", into.get(1).uid());
    }

    @Test
    void basicTransferInfoNamesItsCategoryAndContainer() {
        var info = new legacyconnector.v1_12_2.mezz.jei.transfer.BasicRecipeTransferInfo<>(
                legacyconnector.v1_12_2.net.minecraft.inventory.Container.class, "mod.gizmo", 0, 2, 2, 36);
        assertEquals("mod.gizmo", info.getRecipeCategoryUid());
        assertSame(legacyconnector.v1_12_2.net.minecraft.inventory.Container.class, info.getContainerClass());
        assertTrue(info.canHandle(null));
    }

    @Test
    void vanillaRecipeFactoryMakesAnvilAndBrewingWrappers() {
        var factory = Jei1122Bridge.Helpers.INSTANCE.getVanillaRecipeFactory();
        assertTrue(factory.createAnvilRecipe(null, null, null) instanceof VanillaRecipes.Anvil);
        assertTrue(factory.createBrewingRecipe(null, null, null) instanceof VanillaRecipes.Brewing);
    }

    @Test
    void containsAnyStackToleratesNothing() {
        assertNull(Jei1122Bridge.Helpers.INSTANCE.containsAnyStack(null, List.of()));
        assertNull(Jei1122Bridge.Helpers.INSTANCE.containsAnyStack(List.of(), null));
    }

    @Test
    void craftingGridPlacesSmallRecipesLikeJei4() {
        assertEquals(4, Jei1122Bridge.Helpers.craftingIndex(0, 1, 1));
        assertEquals(0, Jei1122Bridge.Helpers.craftingIndex(0, 3, 3));
        assertEquals(5, Jei1122Bridge.Helpers.craftingIndex(2, 3, 1));
        assertEquals(1, Jei1122Bridge.Helpers.craftingIndex(1, 2, 2));
        assertEquals(3, Jei1122Bridge.Helpers.craftingIndex(2, 2, 2));
    }

    @Test
    void jeiInternalsPluginsReachInto() {
        var map = new ListMultiMap<String, Integer>();
        assertTrue(map.put("a", 1));
        map.put("a", 2);
        assertEquals(2, map.toImmutable().get("a").size());
        assertNotNull(ErrorUtil.checkNotNull("x", "x"));
        assertThrows(NullPointerException.class, () -> ErrorUtil.checkNotNull(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> ErrorUtil.checkNotEmpty(new Object[0], "x"));
        assertNotNull(legacyconnector.v1_12_2.mezz.jei.util.Log.get());
        var area = new legacyconnector.v1_12_2.mezz.jei.gui.recipes.RecipeClickableArea(10, 20, 30, 50, "a", "b");
        assertTrue(area.checkHover(35, 15));
        assertEquals(List.of("a", "b"), area.getRecipeCategoryUids());
    }
}
