package org.kylepitcock.legacyconnector.compat.jei;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.codechicken.nei.recipe.TemplateRecipeHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.compat.nei.NeiBridge;

/**
 * NEI -> JEI bridge, JEI half: every legacy NEI recipe handler becomes a JEI category showing its recipes as NEI drew them
 * (the handler's own {@code drawBackground}/{@code drawForeground}: machine picture, gauges, progress bars, text), with the
 * item slots where the handler put them. Items hidden through NEI are hidden.
 */
@JeiPlugin
public final class LegacyConnectorJeiPlugin implements IModPlugin {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/NEI");
    private static final int WIDTH = 166;
    /** NEI's recipe area: two recipes of 65 pixels a page; a handler showing one recipe a page has the height of both. */
    private static final int HEIGHT = 65;
    /** Handlers whose drawing threw (logged once each). */
    private static final java.util.Set<Class<?>> DRAW_FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final List<Category> categories = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "nei_bridge");
    }

    /**
     * Legacy items of one kind often differ only in their NBT (a filled bucket, a configured part): without this JEI
     * collapses them into one ingredient and reports the rest as duplicates. In the ingredient list the NBT tells them
     * apart; when matching recipes only the legacy metadata does, so changing NBT (charge, damage) cannot break a lookup.
     */
    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyItemHolder)) continue;
            registration.registerSubtypeInterpreter(item, (stack, context) -> {
                StringBuilder data = new StringBuilder();
                Integer meta = stack.get(org.kylepitcock.legacyconnector.core.data.CompatComponents.legacyMeta());
                if (meta != null) data.append('#').append(meta);
                if (context == mezz.jei.api.ingredients.subtypes.UidContext.Ingredient) {
                    var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (custom != null && !custom.isEmpty()) data.append(custom.copyTag());
                }
                return data.isEmpty() ? mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE : data.toString();
            });
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        NeiBridge.loadConfigs();
        categories.clear();
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (TemplateRecipeHandler handler : NeiBridge.templateHandlers()) {
            List<NeiBridge.Recipe> recipes = NeiBridge.recipes(handler);
            String path = "nei_" + handler.getClass().getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            RecipeType<NeiBridge.Recipe> type = RecipeType.create("legacyconnector", path, NeiBridge.Recipe.class);
            String title;
            try {
                title = handler.getRecipeName();
            } catch (RuntimeException | LinkageError e) {
                title = handler.getClass().getSimpleName();
            }
            IDrawable icon = gui.createDrawableItemStack(recipes.isEmpty() || recipes.get(0).outputs().isEmpty()
                    ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE)
                    : recipes.get(0).outputs().get(0).stacks().get(0));
            categories.add(new Category(type, Component.literal(title == null ? path : title), height(handler), icon, recipes, handler));
        }
        if (!categories.isEmpty()) registration.addRecipeCategories(categories.toArray(new IRecipeCategory[0])); // JEI rejects an empty list
    }

    private static int height(TemplateRecipeHandler handler) {
        try {
            return handler.recipiesPerPage() <= 1 ? 2 * HEIGHT : HEIGHT;
        } catch (RuntimeException | LinkageError e) {
            return HEIGHT;
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Category c : categories) if (!c.recipes.isEmpty()) registration.addRecipes(c.type, c.recipes);
    }

    /** NEI asked its handlers per item ("recipes of this", "uses of this"); JEI does the same through this plugin. */
    @Override
    public void registerAdvanced(mezz.jei.api.registration.IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new mezz.jei.api.recipe.advanced.IRecipeManagerPlugin() {
            @Override
            public <V> List<RecipeType<?>> getRecipeTypes(mezz.jei.api.recipe.IFocus<V> focus) {
                List<RecipeType<?>> out = new ArrayList<>();
                net.minecraft.world.item.ItemStack stack = stackOf(focus);
                if (stack == null) return out;
                boolean usage = focus.getRole() == RecipeIngredientRole.INPUT;
                if (focus.getRole() != RecipeIngredientRole.INPUT && focus.getRole() != RecipeIngredientRole.OUTPUT) return out;
                for (Category c : categories) if (!NeiBridge.lookup(c.handler, stack, usage).isEmpty()) out.add(c.type);
                return out;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T, V> List<T> getRecipes(IRecipeCategory<T> category, mezz.jei.api.recipe.IFocus<V> focus) {
                if (!(category instanceof Category c)) return List.of();
                net.minecraft.world.item.ItemStack stack = stackOf(focus);
                if (stack == null) return List.of();
                if (focus.getRole() != RecipeIngredientRole.INPUT && focus.getRole() != RecipeIngredientRole.OUTPUT) return List.of();
                return (List<T>) NeiBridge.lookup(c.handler, stack, focus.getRole() == RecipeIngredientRole.INPUT);
            }

            @Override
            public <T> List<T> getRecipes(IRecipeCategory<T> category) {
                return List.of();
            }
        });
    }

    private static net.minecraft.world.item.ItemStack stackOf(mezz.jei.api.recipe.IFocus<?> focus) {
        Object value = focus.getTypedValue().getIngredient();
        return value instanceof net.minecraft.world.item.ItemStack s ? s : null;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<net.minecraft.world.item.ItemStack> hidden = NeiBridge.hiddenItems();
        if (!hidden.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
    }

    private record Category(RecipeType<NeiBridge.Recipe> type, Component title, int height, IDrawable icon,
                            List<NeiBridge.Recipe> recipes, TemplateRecipeHandler handler) implements IRecipeCategory<NeiBridge.Recipe> {
        @Override
        public RecipeType<NeiBridge.Recipe> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, NeiBridge.Recipe recipe, IFocusGroup focuses) {
            for (NeiBridge.Slot s : recipe.inputs()) builder.addSlot(RecipeIngredientRole.INPUT, s.x(), s.y()).addItemStacks(s.stacks());
            for (NeiBridge.Slot s : recipe.outputs()) builder.addSlot(RecipeIngredientRole.OUTPUT, s.x(), s.y()).addItemStacks(s.stacks());
            for (NeiBridge.Slot s : recipe.others()) builder.addSlot(RecipeIngredientRole.CATALYST, s.x(), s.y()).addItemStacks(s.stacks());
        }

        /**
         * NEI's GuiRecipe translated to the recipe's origin and called the handler's {@code drawBackground(i)} and
         * {@code drawForeground(i)} (which binds the GUI texture and calls {@code drawExtras(i)}); JEI's pose is at the same
         * origin here. They run as legacy GUI drawing (GL11, Tessellator, texture binds through the legacy render context), so
         * handlers that bind their texture themselves (DragonAPI's {@code ReikaTextureHelper}, raw GL ids) draw as in their
         * machine GUIs. NEI advanced {@code cycleticks} every tick: progress bars animate from the clock.
         */
        @Override
        public void draw(NeiBridge.Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            TemplateRecipeHandler h = recipe.handler();
            if (h == null) return;
            h.cycleticks = (int) (net.minecraft.Util.getMillis() / 50L);
            org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.beginGui(graphics);
            try {
                h.drawBackground(recipe.index());
                h.drawForeground(recipe.index());
            } catch (RuntimeException | LinkageError e) {
                if (DRAW_FAILED.add(h.getClass())) LOG.warn("NEI handler {} failed to draw a recipe: {}", h.getClass().getName(), e.toString());
            } finally {
                org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.end();
            }
        }
    }
}
