package org.kylepitcock.legacyconnector.compat.nei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.data.CompatComponents;

class NeiRecipeIdsTest {
    private static java.lang.reflect.Field componentHolder;
    private static Object previousComponent;

    @BeforeAll
    static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        componentHolder = DeferredHolder.class.getDeclaredField("holder");
        componentHolder.setAccessible(true);
        previousComponent = componentHolder.get(CompatComponents.LEGACY_META);
        if (previousComponent == null) {
            componentHolder.set(CompatComponents.LEGACY_META, Holder.direct(DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT).build()));
        }
    }

    @AfterAll
    static void restoreComponent() throws Exception {
        if (componentHolder != null) componentHolder.set(CompatComponents.LEGACY_META, previousComponent);
    }

    @Test
    void enumeratedRecipesGetStableDistinctIdsAcrossTransferIds() {
        var handler = new ExampleHandler();
        var all = NeiBridge.recipes(handler);
        assertEquals(3, all.size());
        assertTrue(all.stream().allMatch(recipe -> recipe.id() != null));
        assertEquals(3, all.stream().map(NeiBridge.Recipe::id).distinct().count());
        assertEquals(all.stream().map(NeiBridge.Recipe::id).toList(),
                NeiBridge.recipes(new ExampleHandler()).stream().map(NeiBridge.Recipe::id).toList());

        assertEquals(1, all.get(1).index());
        assertEquals(0, all.get(2).index());
    }

    @Test
    void fingerprintsUseAllAlternativesAndLegacyStackData() {
        var stick = new ItemStack(Items.STICK);
        var coal = new ItemStack(Items.COAL);
        var torch = new ItemStack(Items.TORCH);
        ResourceLocation base = id(List.of(stick, coal));
        assertNotNull(base);
        assertEquals(base, id(List.of(coal, stick.copy())));
        assertNotEquals(base, id(List.of(stick, torch)));
        assertNotEquals(base, id(List.of(stick, coal.copyWithCount(2))));

        var variant = coal.copy();
        variant.set(CompatComponents.legacyMeta(), 1);
        assertNotEquals(base, id(List.of(stick, variant)));

        var tag = new CompoundTag();
        tag.putInt("variant", 2);
        variant = coal.copy();
        variant.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        assertNotEquals(base, id(List.of(stick, variant)));
    }

    @Test
    void separatesHandlersAndSlotRolesEvenForEqualContents() {
        var slots = List.of(new NeiBridge.Slot(10, 20, List.of(new ItemStack(Items.STICK))));
        var base = new NeiRecipeIds(ExampleHandler.class).next(slots, List.of(), List.of());
        assertNotEquals(base, new NeiRecipeIds(NeiRecipeIdsTest.class).next(slots, List.of(), List.of()));
        assertNotEquals(base, new NeiRecipeIds(ExampleHandler.class).next(List.of(), slots, List.of()));
        assertNotEquals(base, new NeiRecipeIds(ExampleHandler.class).next(List.of(), List.of(), slots));
    }

    private static ResourceLocation id(List<ItemStack> alternatives) {
        return new NeiRecipeIds(ExampleHandler.class).next(
                List.of(new NeiBridge.Slot(10, 20, alternatives)), List.of(), List.of());
    }

    private static final class ExampleHandler extends TemplateRecipeHandler {
        @Override public String getRecipeName() { return "Example"; }
        @Override public String getGuiTexture() { return null; }
        @Override public String getOverlayIdentifier() { return "example"; }

        @Override
        public void loadTransferRects() {
            transferRects.add(new RecipeTransferRect(new Rectangle(), "another_page"));
        }

        @Override
        public void loadCraftingRecipes(String outputId, Object... results) {
            if ("example".equals(outputId)) addRecipe(legacyconnector.v1_7_10.net.minecraft.init.Items.coal);
            addRecipe(legacyconnector.v1_7_10.net.minecraft.init.Items.diamond);
        }

        private void addRecipe(legacyconnector.v1_7_10.net.minecraft.item.Item result) {
            arecipes.add(new CachedRecipe() {
                @Override public PositionedStack getIngredient() {
                    return positioned(legacyconnector.v1_7_10.net.minecraft.init.Items.stick, 10);
                }
                @Override public PositionedStack getResult() { return positioned(result, 60); }
            });
        }

        private static PositionedStack positioned(legacyconnector.v1_7_10.net.minecraft.item.Item item, int x) {
            return new PositionedStack(new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item), x, 20, false);
        }
    }
}
