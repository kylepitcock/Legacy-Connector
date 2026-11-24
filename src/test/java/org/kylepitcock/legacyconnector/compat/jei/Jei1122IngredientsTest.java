package org.kylepitcock.legacyconnector.compat.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientHelper;
import legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRenderer;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType;
import legacyconnector.v1_12_2.mezz.jei.plugins.vanilla.crafting.ShapedRecipesWrapper;
import legacyconnector.v1_12_2.mezz.jei.plugins.vanilla.crafting.ShapelessRecipeWrapper;
import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.ShapedRecipes;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A mod's own ingredient type becomes a modern JEI type, and a plugin's vanilla-category recipes become modern recipes. */
class Jei1122IngredientsTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    /** A mod's ingredient: a gas. */
    record Gas(String name) {}

    static final IIngredientHelper<Gas> HELPER = new IIngredientHelper<>() {
        @Override
        public String getDisplayName(Gas g) {
            return "Gas " + g.name();
        }

        @Override
        public String getUniqueId(Gas g) {
            return g.name();
        }

        @Override
        public String getModId(Gas g) {
            return "Some Mod";
        }

        @Override
        public String getErrorInfo(Gas g) {
            return g.name();
        }
    };

    @SuppressWarnings("unchecked")
    @Test
    void modIngredientTypeIsRegisteredWithTheModernJei() {
        Object[] seen = new Object[5];
        var registration = (mezz.jei.api.registration.IModIngredientRegistration) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {mezz.jei.api.registration.IModIngredientRegistration.class}, (proxy, method, args) -> {
                    if (method.getName().equals("register")) System.arraycopy(args, 0, seen, 0, args.length);
                    return null;
                });
        IIngredientRenderer<Gas> renderer = new IIngredientRenderer<>() {
            @Override
            public void render(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y, Gas g) {}

            @Override
            public List<String> getTooltip(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, Gas g, legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flag) {
                return List.of(g.name());
            }
        };
        ModIngredientTypes.clear();
        ModIngredientTypes.register(registration, Gas.class, HELPER, renderer, List.of(new Gas("hydrogen"), new Gas("oxygen")));
        var type = (mezz.jei.api.ingredients.IIngredientType<Gas>) seen[0];
        assertSame(Gas.class, type.getIngredientClass());
        var helper = (mezz.jei.api.ingredients.IIngredientHelper<Gas>) seen[2];
        assertEquals("Gas hydrogen", helper.getDisplayName(new Gas("hydrogen")));
        assertEquals("oxygen", helper.getUniqueId(new Gas("oxygen"), mezz.jei.api.ingredients.subtypes.UidContext.Ingredient));
        assertEquals("some_mod:hydrogen", helper.getResourceLocation(new Gas("hydrogen")).toString());
        assertSame(type, helper.getIngredientType());
        var codec = (com.mojang.serialization.Codec<Gas>) seen[4];
        var json = codec.encodeStart(JsonOps.INSTANCE, new Gas("oxygen")).getOrThrow();
        assertEquals("oxygen", json.getAsString());
        assertEquals(new Gas("oxygen"), codec.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertTrue(codec.parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("neon")).error().isPresent());
        assertNotNull(ModIngredientTypes.of(Gas.class));
        assertNull(ModIngredientTypes.of(String.class));
    }

    @Test
    void layoutGivesAGroupPerBridgedType() {
        var registration = (mezz.jei.api.registration.IModIngredientRegistration) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {mezz.jei.api.registration.IModIngredientRegistration.class}, (proxy, method, args) -> null);
        ModIngredientTypes.clear();
        ModIngredientTypes.register(registration, Gas.class, HELPER, new IIngredientRenderer<Gas>() {
            @Override
            public void render(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y, Gas g) {}

            @Override
            public List<String> getTooltip(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, Gas g, legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag flag) {
                return List.of();
            }
        }, new ArrayList<Gas>());
        var layout = new Jei1122Bridge.Layout(null);
        var ingredients = new Jei1122Bridge.Ingredients();
        IIngredientType<Gas> type = () -> Gas.class;
        ingredients.setInput(type, new Gas("hydrogen"));
        ingredients.setOutput(type, new Gas("oxygen"));
        var group = layout.getIngredientsGroup(type);
        group.init(0, true, null, 2, 3, 16, 40, 1, 2);
        group.init(1, false, null, 30, 3, 16, 40, 1, 2);
        group.set(ingredients);
        assertSame(group, layout.getIngredientsGroup(Gas.class));
        assertEquals(1, layout.others.size());
        var slots = ((Jei1122Bridge.Group<Gas>) group).slots;
        assertEquals(new Gas("hydrogen"), slots.get(0).getDisplayedIngredient());
        assertEquals(new Gas("oxygen"), slots.get(1).getDisplayedIngredient());
        assertEquals(1, slots.get(0).insetX);
        assertEquals(2, slots.get(0).insetY);
        assertTrue(layout.items.slots.isEmpty());
    }

    @Test
    void shapedAndShapelessCraftingWrappersBecomeModernCraftingRecipes() {
        var list = NonNullList.<Ingredient>withSize(4, Ingredient.EMPTY);
        for (int i = 0; i < 4; i++) list.set(i, Ingredient.fromStacks(new ItemStack(Items.STICK)));
        var shaped = new ShapedRecipes("", 2, 2, list, new ItemStack(Items.APPLE));
        var holder = VanillaRecipes.crafting(new ShapedRecipesWrapper(null, shaped), 0);
        assertNotNull(holder);
        assertTrue(holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe);
        var recipe = (net.minecraft.world.item.crafting.ShapedRecipe) holder.value();
        assertEquals(2, recipe.getWidth());
        assertEquals(2, recipe.getHeight());
        assertEquals(net.minecraft.world.item.Items.APPLE, recipe.getResultItem(null).getItem());
        assertEquals("legacyconnector", holder.id().getNamespace());

        var shapeless = VanillaRecipes.crafting(new ShapelessRecipeWrapper(null, shaped), 1);
        assertNotNull(shapeless);
        assertTrue(shapeless.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe, "a wrapper that is not shaped gives a shapeless recipe");
    }

    @Test
    void smeltingWrapperBecomesAModernSmeltingRecipe() {
        var wrapper = new legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper() {
            @Override
            public void getIngredients(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients in) {
                in.setInput(ItemStack.class, new ItemStack(Items.STICK));
                in.setOutput(ItemStack.class, new ItemStack(Items.APPLE));
            }
        };
        var holder = VanillaRecipes.smelting(wrapper, 0);
        assertNotNull(holder);
        assertEquals(net.minecraft.world.item.Items.APPLE, holder.value().getResultItem(null).getItem());
        assertTrue(holder.value().getIngredients().get(0).test(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)));
    }
}
