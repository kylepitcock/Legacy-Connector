package org.kylepitcock.legacyconnector.compat.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.mezz.jei.api.IModPlugin;
import legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawable;
import legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableAnimated;
import legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableStatic;
import legacyconnector.v1_12_2.mezz.jei.api.gui.ITickTimer;
import legacyconnector.v1_12_2.mezz.jei.api.ingredients.VanillaTypes;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IIngredientType;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapper;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeWrapperFactory;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.FluidBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * JEI 4 (Minecraft 1.12.2) -> modern JEI. JEI found 1.12 plugins by their {@code @JEIPlugin} annotation and asked them for categories, recipes,
 * catalysts and click areas; this plugin does the same with the legacy plugins and gives the modern JEI what they registered: each legacy
 * category becomes a modern one that draws as the legacy one did (its background, {@code drawExtras}, the wrapper's {@code drawInfo}, the
 * legacy drawables, all through the legacy GUI drawing) with the slots the category's {@code setRecipe} laid out. Catalysts, recipe click
 * areas of the mod's GUIs, ingredient info, extra GUI areas and hidden ingredients follow.
 * Not bridged: recipes a legacy plugin adds to the vanilla categories (the layer converts the mods' crafting recipes itself), recipe transfer,
 * ingredient types of the mods' own.
 */
@JeiPlugin
public final class Jei1122Bridge implements mezz.jei.api.IModPlugin {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/JEI1122");
    private static final java.util.Set<Class<?>> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final List<IModPlugin> plugins = new ArrayList<>();
    private boolean loaded;
    private final List<Category> categories = new ArrayList<>();
    private final Registry registry = new Registry();
    /** The bridge JEI created, for the legacy runtime's recipe lookups. */
    static volatile Jei1122Bridge current;

    public Jei1122Bridge() {
        current = this;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "jei1122_bridge");
    }

    private void loadPlugins() {
        if (loaded) return;
        loaded = true;
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        try {
            for (var data : legacyconnector.v1_12_2.net.minecraftforge.fml.common.discovery.ASMDataTable.mcc$get().getAll("mezz.jei.api.JEIPlugin")) names.add(data.getClassName());
        } catch (RuntimeException | LinkageError e) {
            LOG.debug("No 1.12.2 annotation table: {}", e.toString());
            return;
        }
        for (String name : names) {
            try {
                Class<?> c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
                if (!IModPlugin.class.isAssignableFrom(c)) continue;
                var ctor = c.getDeclaredConstructor();
                ctor.setAccessible(true);
                plugins.add((IModPlugin) ctor.newInstance());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.warn("1.12.2 JEI plugin {} could not be created: {}", name, e.toString());
            }
        }
        if (!plugins.isEmpty()) LOG.info("1.12.2 JEI plugins: {}", plugins.stream().map(p -> p.getClass().getName()).toList());
    }

    private static void guard(IModPlugin plugin, String what, Runnable run) {
        try {
            run.run();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("1.12.2 JEI plugin {} failed in {}", plugin.getClass().getName(), what, e);
        }
    }

    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        loadPlugins();
        // the layer gives every legacy item its subtype interpreter (LegacyConnectorJeiPlugin); a 1.12.2 subtype is a modern item of its own
        for (IModPlugin p : plugins) guard(p, "registerItemSubtypes", () -> p.registerItemSubtypes(Helpers.SUBTYPES));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        loadPlugins();
        categories.clear();
        registry.clear();
        Helpers.modernGui = registration.getJeiHelpers().getGuiHelper();
        List<IRecipeCategory<?>> legacy = new ArrayList<>();
        for (IModPlugin p : plugins) {
            guard(p, "registerIngredients", () -> p.registerIngredients(() -> Helpers.INSTANCE));
            guard(p, "registerCategories", () -> p.registerCategories(new legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategoryRegistration() {
                @Override
                public void addRecipeCategories(IRecipeCategory<?>... recipeCategories) {
                    for (var c : recipeCategories) if (c != null) legacy.add(c);
                }

                @Override
                public legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers() {
                    return Helpers.INSTANCE;
                }
            }));
        }
        // JEI asks for catalysts before recipes; JEI 4 had both from register(), so it runs now
        for (IModPlugin p : plugins) guard(p, "register", () -> p.register(registry));
        java.util.Set<String> paths = new java.util.HashSet<>();
        for (IRecipeCategory<?> c : legacy) {
            try {
                String uid = c.getUid();
                String ns = sanitize(c.getModName() == null || c.getModName().isBlank() ? "legacy" : c.getModName());
                String path = sanitize(uid);
                if (!paths.add(ns + ":" + path)) continue;
                @SuppressWarnings("unchecked")
                var cat = (IRecipeCategory<IRecipeWrapper>) c;
                categories.add(new Category(cat, RecipeType.create(ns, path, Recipe.class), uid));
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("1.12.2 JEI category {} could not be added: {}", c.getClass().getName(), e.toString());
            }
        }
        for (Category c : categories) c.recipes.addAll(wrappers(c));
        for (Category c : categories) c.icon = icon(c, registration.getJeiHelpers().getGuiHelper());
        if (!categories.isEmpty()) {
            registration.addRecipeCategories(categories.toArray(new mezz.jei.api.recipe.category.IRecipeCategory[0]));
            LOG.info("1.12.2 JEI categories: {}", categories.stream().map(c -> c.uid + " (" + c.recipes.size() + ")").toList());
        }
    }

    /** The recipes added for a category as wrappers (a recipe object of the mod goes through the factory it registered with handleRecipes). */
    private List<Recipe> wrappers(Category c) {
        List<Recipe> out = new ArrayList<>();
        for (Object o : registry.recipes.getOrDefault(c.uid, List.of())) {
            IRecipeWrapper w = null;
            try {
                if (o instanceof IRecipeWrapper rw) {
                    w = rw;
                } else if (o != null) {
                    for (var h : registry.handlers) {
                        if (h.uid.equals(c.uid) && h.recipeClass.isInstance(o)) {
                            @SuppressWarnings("unchecked")
                            var factory = (IRecipeWrapperFactory<Object>) h.factory;
                            w = factory.getRecipeWrapper(o);
                            break;
                        }
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                if (FAILED.add(o.getClass())) LOG.warn("1.12.2 JEI recipe {} of {} has no wrapper: {}", o.getClass().getName(), c.uid, e.toString());
            }
            if (w != null) out.add(new Recipe(w, c));
            else if (o != null && FAILED.add(o.getClass())) LOG.warn("1.12.2 JEI recipe {} of {} is no recipe wrapper and no handler takes it; dropped", o.getClass().getName(), c.uid);
        }
        return out;
    }

    private mezz.jei.api.gui.drawable.IDrawable icon(Category c, mezz.jei.api.helpers.IGuiHelper gui) {
        try {
            IDrawable own = c.legacy.getIcon();
            if (own != null) return modern(own);
        } catch (RuntimeException | LinkageError e) {
            // the catalyst below
        }
        for (var cat : registry.catalysts) {
            if (cat.uids.contains(c.uid) && cat.ingredient instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s && !s.isEmpty()) {
                return gui.createDrawableItemStack(Stacks1122.toModern(s));
            }
        }
        return gui.createDrawableItemStack(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE));
    }

    private static String sanitize(String s) {
        String out = s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return out.isEmpty() ? "category" : out;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var cat : registry.catalysts) {
            List<RecipeType<?>> types = new ArrayList<>();
            for (String uid : cat.uids) {
                RecipeType<?> t = typeOf(uid);
                if (t != null) types.add(t);
            }
            if (types.isEmpty()) continue;
            try {
                if (cat.ingredient instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s && !s.isEmpty()) {
                    registration.addRecipeCatalyst(Stacks1122.toModern(s), types.toArray(new RecipeType[0]));
                }
            } catch (RuntimeException e) {
                LOG.debug("JEI refused a 1.12.2 catalyst for {}: {}", cat.uids, e.toString());
            }
        }
    }

    /** The modern type of a legacy category uid: a bridged category, or one of the vanilla ones. */
    private RecipeType<?> typeOf(String uid) {
        for (Category c : categories) if (c.uid.equals(uid)) return c.type;
        return switch (uid) {
            case legacyconnector.v1_12_2.mezz.jei.api.recipe.VanillaRecipeCategoryUid.CRAFTING -> RecipeTypes.CRAFTING;
            case legacyconnector.v1_12_2.mezz.jei.api.recipe.VanillaRecipeCategoryUid.SMELTING -> RecipeTypes.SMELTING;
            case legacyconnector.v1_12_2.mezz.jei.api.recipe.VanillaRecipeCategoryUid.FUEL -> RecipeTypes.FUELING;
            case legacyconnector.v1_12_2.mezz.jei.api.recipe.VanillaRecipeCategoryUid.BREWING -> RecipeTypes.BREWING;
            case legacyconnector.v1_12_2.mezz.jei.api.recipe.VanillaRecipeCategoryUid.ANVIL -> RecipeTypes.ANVIL;
            default -> null;
        };
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Category c : categories) if (!c.recipes.isEmpty()) registration.addRecipes(c.type, c.recipes);
        for (var info : registry.info) {
            Component[] lines = new Component[info.keys.length];
            for (int i = 0; i < lines.length; i++) lines[i] = Component.translatable(info.keys[i]);
            List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
            List<net.neoforged.neoforge.fluids.FluidStack> fluids = new ArrayList<>();
            for (Object o : info.ingredients) {
                if (o instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s && !s.isEmpty()) items.add(Stacks1122.toModern(s));
                else if (o instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack f) fluids.add(FluidBridge1122.toModern(f));
            }
            try {
                if (!items.isEmpty()) registration.addItemStackInfo(items, lines);
                if (!fluids.isEmpty()) registration.addIngredientInfo(fluids, NeoForgeTypes.FLUID_STACK, lines);
            } catch (RuntimeException e) {
                LOG.debug("JEI refused 1.12.2 ingredient info: {}", e.toString());
            }
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (registry.clickAreas.isEmpty() && registry.guiHandlers.isEmpty()) return;
        registration.addGuiContainerHandler(org.kylepitcock.legacyconnector.v1_12_2.client.LegacyContainerScreen1122.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<org.kylepitcock.legacyconnector.v1_12_2.client.LegacyContainerScreen1122>() {
                    @Override
                    public java.util.Collection<mezz.jei.api.gui.handlers.IGuiClickableArea> getGuiClickableAreas(
                            org.kylepitcock.legacyconnector.v1_12_2.client.LegacyContainerScreen1122 screen, double mouseX, double mouseY) {
                        var gui = screen.legacy();
                        if (gui == null) return List.of();
                        List<mezz.jei.api.gui.handlers.IGuiClickableArea> out = new ArrayList<>();
                        for (var a : registry.clickAreas) {
                            if (!a.guiClass.isInstance(gui)) continue;
                            List<RecipeType<?>> types = new ArrayList<>();
                            for (String uid : a.uids) {
                                RecipeType<?> t = typeOf(uid);
                                if (t != null) types.add(t);
                            }
                            if (!types.isEmpty()) out.add(mezz.jei.api.gui.handlers.IGuiClickableArea.createBasic(a.x, a.y, a.w, a.h, types.toArray(new RecipeType[0])));
                        }
                        return out;
                    }

                    @Override
                    public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(org.kylepitcock.legacyconnector.v1_12_2.client.LegacyContainerScreen1122 screen) {
                        var gui = screen.legacy();
                        if (gui == null) return List.of();
                        List<net.minecraft.client.renderer.Rect2i> out = new ArrayList<>();
                        for (var h : registry.guiHandlers) {
                            try {
                                if (!h.getGuiContainerClass().isInstance(gui)) continue;
                                @SuppressWarnings("unchecked")
                                var handler = (legacyconnector.v1_12_2.mezz.jei.api.gui.IAdvancedGuiHandler<legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer>) h;
                                var areas = handler.getGuiExtraAreas(gui);
                                if (areas != null) for (var r : areas) out.add(new net.minecraft.client.renderer.Rect2i(r.x, r.y, r.width, r.height));
                            } catch (RuntimeException | LinkageError e) {
                                if (FAILED.add(h.getClass())) LOG.warn("1.12.2 JEI GUI handler {} failed: {}", h.getClass().getName(), e.toString());
                            }
                        }
                        return out;
                    }
                });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        Helpers.runtime = runtime;
        List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
        List<net.neoforged.neoforge.fluids.FluidStack> fluids = new ArrayList<>();
        for (Object o : Helpers.BLACKLIST) {
            if (o instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s && !s.isEmpty()) items.add(Stacks1122.toModern(s));
            else if (o instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack f) fluids.add(FluidBridge1122.toModern(f));
        }
        try {
            if (!items.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, items);
            if (!fluids.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(NeoForgeTypes.FLUID_STACK, fluids);
        } catch (RuntimeException e) {
            LOG.debug("JEI refused to hide 1.12.2 ingredients: {}", e.toString());
        }
        for (IModPlugin p : plugins) guard(p, "onRuntimeAvailable", () -> p.onRuntimeAvailable(Helpers.RUNTIME));
    }

    // ------------------------------------------------------------------ what the legacy plugins register

    record Handler(Class<?> recipeClass, IRecipeWrapperFactory<?> factory, String uid) {}

    record Catalyst(Object ingredient, List<String> uids) {}

    record ClickArea(Class<?> guiClass, int x, int y, int w, int h, String[] uids) {}

    record Info(List<?> ingredients, String[] keys) {}

    static final class Registry implements legacyconnector.v1_12_2.mezz.jei.api.IModRegistry {
        final Map<String, List<Object>> recipes = new LinkedHashMap<>();
        final List<Handler> handlers = new ArrayList<>();
        final List<Catalyst> catalysts = new ArrayList<>();
        final List<ClickArea> clickAreas = new ArrayList<>();
        final List<Info> info = new ArrayList<>();
        final List<legacyconnector.v1_12_2.mezz.jei.api.gui.IAdvancedGuiHandler<?>> guiHandlers = new ArrayList<>();

        void clear() {
            recipes.clear();
            handlers.clear();
            catalysts.clear();
            clickAreas.clear();
            info.clear();
            guiHandlers.clear();
        }

        @Override
        public legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers() {
            return Helpers.INSTANCE;
        }

        @Override
        public legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRegistry getIngredientRegistry() {
            return Helpers.INGREDIENTS;
        }

        @Override
        public void addRecipes(java.util.Collection<?> list, String uid) {
            if (list != null && uid != null) recipes.computeIfAbsent(uid, k -> new ArrayList<>()).addAll(list);
        }

        @Override
        public <T> void handleRecipes(Class<T> recipeClass, IRecipeWrapperFactory<T> factory, String uid) {
            if (recipeClass != null && factory != null && uid != null) handlers.add(new Handler(recipeClass, factory, uid));
        }

        @Override
        public void addRecipeClickArea(Class<? extends legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer> guiClass, int x, int y, int w, int h, String... uids) {
            if (guiClass != null && uids != null) clickAreas.add(new ClickArea(guiClass, x, y, w, h, uids));
        }

        @Override
        public void addRecipeCatalyst(Object ingredient, String... uids) {
            if (ingredient != null && uids != null) catalysts.add(new Catalyst(ingredient, List.of(uids)));
        }

        @Override
        public <T> void addIngredientInfo(T ingredient, IIngredientType<T> type, String... keys) {
            info.add(new Info(List.of(ingredient), keys));
        }

        @Override
        public <T> void addIngredientInfo(List<T> ingredients, IIngredientType<T> type, String... keys) {
            info.add(new Info(List.copyOf(ingredients), keys));
        }

        @Override
        public <T> void addIngredientInfo(T ingredient, Class<? extends T> cls, String... keys) {
            info.add(new Info(List.of(ingredient), keys));
        }

        @Override
        public <T> void addIngredientInfo(List<T> ingredients, Class<? extends T> cls, String... keys) {
            info.add(new Info(List.copyOf(ingredients), keys));
        }

        @Override
        public void addAdvancedGuiHandlers(legacyconnector.v1_12_2.mezz.jei.api.gui.IAdvancedGuiHandler<?>... list) {
            if (list != null) for (var h : list) if (h != null) guiHandlers.add(h);
        }

        @Override
        public legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferRegistry getRecipeTransferRegistry() {
            return (containerClass, uid, recipeSlotStart, recipeSlotCount, inventorySlotStart, inventorySlotCount) -> {};
        }
    }

    // ------------------------------------------------------------------ categories

    /** One shown legacy recipe: its wrapper and its category. */
    record Recipe(IRecipeWrapper wrapper, Category category) {}

    static final class Category implements mezz.jei.api.recipe.category.IRecipeCategory<Recipe> {
        final IRecipeCategory<IRecipeWrapper> legacy;
        final RecipeType<Recipe> type;
        final String uid;
        final List<Recipe> recipes = new ArrayList<>();
        final IDrawable background;
        final Component title;
        mezz.jei.api.gui.drawable.IDrawable icon;

        Category(IRecipeCategory<IRecipeWrapper> legacy, RecipeType<Recipe> type, String uid) {
            this.legacy = legacy;
            this.type = type;
            this.uid = uid;
            this.background = legacy.getBackground();
            String t = legacy.getTitle();
            this.title = Component.literal(t == null ? uid : t);
        }

        @Override
        public RecipeType<Recipe> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public int getWidth() {
            return background == null ? 160 : Math.max(1, background.getWidth());
        }

        @Override
        public int getHeight() {
            return background == null ? 60 : Math.max(1, background.getHeight());
        }

        @Override
        public mezz.jei.api.gui.drawable.IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
            Ingredients ingredients = new Ingredients();
            Layout layout = new Layout();
            try {
                recipe.wrapper().getIngredients(ingredients);
                if (recipe.wrapper() instanceof legacyconnector.v1_12_2.mezz.jei.api.recipe.wrapper.ICustomCraftingRecipeWrapper custom) {
                    custom.setRecipe(layout, ingredients);
                } else {
                    legacy.setRecipe(layout, recipe.wrapper(), ingredients);
                }
            } catch (RuntimeException | LinkageError e) {
                if (FAILED.add(legacy.getClass())) LOG.warn("1.12.2 JEI category {} failed to lay out a recipe", uid, e);
            }
            layout.emit(builder);
        }

        /** JEI 4 drew the background at the recipe's origin, then the category's extras and the wrapper's info, as legacy GUI drawing. */
        @Override
        public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            var mc = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
            RenderContext.beginGui(graphics);
            try {
                legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                if (background != null) background.draw(mc, 0, 0);
                legacy.drawExtras(mc);
                recipe.wrapper().drawInfo(mc, getWidth(), getHeight(), (int) mouseX, (int) mouseY);
            } catch (RuntimeException | LinkageError e) {
                if (FAILED.add(recipe.wrapper().getClass())) LOG.warn("1.12.2 JEI category {} failed to draw a recipe", uid, e);
            } finally {
                RenderContext.end();
            }
        }

        @Override
        public List<Component> getTooltipStrings(Recipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            List<Component> out = new ArrayList<>();
            try {
                var own = legacy.getTooltipStrings((int) mouseX, (int) mouseY);
                if (own != null) for (String s : own) out.add(Component.literal(s));
                var wrapper = recipe.wrapper().getTooltipStrings((int) mouseX, (int) mouseY);
                if (wrapper != null) for (String s : wrapper) out.add(Component.literal(s));
            } catch (RuntimeException | LinkageError e) {
                if (FAILED.add(recipe.wrapper().getClass())) LOG.debug("1.12.2 JEI tooltip of {} failed: {}", uid, e.toString());
            }
            return out;
        }
    }

    // ------------------------------------------------------------------ ingredients and slots

    static final class Ingredients implements legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients {
        final Map<Class<?>, List<List<?>>> inputs = new LinkedHashMap<>();
        final Map<Class<?>, List<List<?>>> outputs = new LinkedHashMap<>();

        private static Class<?> key(Class<?> c) {
            if (c != null && legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class.isAssignableFrom(c)) return legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class;
            if (c != null && legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.class.isAssignableFrom(c)) return legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.class;
            return c;
        }

        private static List<List<?>> lists(List<? extends List<?>> in) {
            List<List<?>> out = new ArrayList<>();
            if (in != null) for (List<?> l : in) out.add(l == null ? new ArrayList<>() : new ArrayList<>(l));
            return out;
        }

        private static List<List<?>> singles(List<?> in) {
            List<List<?>> out = new ArrayList<>();
            if (in != null) for (Object o : in) out.add(o == null ? new ArrayList<>() : new ArrayList<>(List.of(o)));
            return out;
        }

        @Override public <T> void setInput(IIngredientType<T> t, T v) { setInput(t.getIngredientClass(), v); }
        @Override public <T> void setInputs(IIngredientType<T> t, List<T> v) { setInputs(t.getIngredientClass(), v); }
        @Override public <T> void setInputLists(IIngredientType<T> t, List<List<T>> v) { setInputLists(t.getIngredientClass(), v); }
        @Override public <T> void setOutput(IIngredientType<T> t, T v) { setOutput(t.getIngredientClass(), v); }
        @Override public <T> void setOutputs(IIngredientType<T> t, List<T> v) { setOutputs(t.getIngredientClass(), v); }
        @Override public <T> void setOutputLists(IIngredientType<T> t, List<List<T>> v) { setOutputLists(t.getIngredientClass(), v); }
        @Override public <T> List<List<T>> getInputs(IIngredientType<T> t) { return getInputs(t.getIngredientClass()); }
        @Override public <T> List<List<T>> getOutputs(IIngredientType<T> t) { return getOutputs(t.getIngredientClass()); }

        @Override
        public void setInputIngredients(List<legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient> list) {
            List<List<?>> out = new ArrayList<>();
            if (list != null) for (var i : list) out.add(i == null ? new ArrayList<>() : new ArrayList<>(List.of(i.getMatchingStacks())));
            inputs.put(legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class, out);
        }

        @Override public <T> void setInput(Class<? extends T> c, T v) { inputs.put(key(c), singles(java.util.Collections.singletonList(v))); }
        @Override public <T> void setInputs(Class<? extends T> c, List<T> v) { inputs.put(key(c), singles(v)); }
        @Override public <T> void setInputLists(Class<? extends T> c, List<List<T>> v) { inputs.put(key(c), lists(v)); }
        @Override public <T> void setOutput(Class<? extends T> c, T v) { outputs.put(key(c), singles(java.util.Collections.singletonList(v))); }
        @Override public <T> void setOutputs(Class<? extends T> c, List<T> v) { outputs.put(key(c), singles(v)); }
        @Override public <T> void setOutputLists(Class<? extends T> c, List<List<T>> v) { outputs.put(key(c), lists(v)); }

        @SuppressWarnings("unchecked")
        @Override
        public <T> List<List<T>> getInputs(Class<? extends T> c) {
            return (List<List<T>>) (List<?>) inputs.getOrDefault(key(c), new ArrayList<>());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> List<List<T>> getOutputs(Class<? extends T> c) {
            return (List<List<T>>) (List<?>) outputs.getOrDefault(key(c), new ArrayList<>());
        }
    }

    /** One slot of a legacy layout. */
    static final class Slot<T> implements legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredient<T> {
        final boolean input;
        final int x, y, w, h;
        int capacity;
        boolean showCapacity;
        IDrawable overlay;
        IDrawable background;
        final List<T> stacks = new ArrayList<>();

        Slot(boolean input, int x, int y, int w, int h) {
            this.input = input;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        @Override public List<T> getAllIngredients() { return stacks; }
        @Override public T getDisplayedIngredient() { return stacks.isEmpty() ? null : stacks.get(0); }
        @Override public boolean isInput() { return input; }
    }

    /** The slots of one ingredient type, as JEI 4's GuiIngredientGroup kept them. */
    static class Group<T> implements legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredientGroup<T> {
        final Class<?> kind;
        final java.util.TreeMap<Integer, Slot<T>> slots = new java.util.TreeMap<>();
        final List<legacyconnector.v1_12_2.mezz.jei.api.gui.ITooltipCallback<T>> tooltips = new ArrayList<>();

        Group(Class<?> kind) {
            this.kind = kind;
        }

        /** JEI 4 set the recipe's inputs into the input slots and its outputs into the output slots, both in slot order. */
        @SuppressWarnings("unchecked")
        @Override
        public void set(legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredients ingredients) {
            if (!(ingredients instanceof Ingredients ing)) return;
            List<List<?>> in = ing.inputs.getOrDefault(kind, List.of());
            List<List<?>> out = ing.outputs.getOrDefault(kind, List.of());
            int i = 0, o = 0;
            for (var e : slots.entrySet()) {
                if (e.getValue().input) {
                    if (i < in.size()) set(e.getKey(), (List<T>) in.get(i));
                    i++;
                } else {
                    if (o < out.size()) set(e.getKey(), (List<T>) out.get(o));
                    o++;
                }
            }
        }

        @Override
        public void set(int slot, List<T> list) {
            Slot<T> s = slots.get(slot);
            if (s == null) return;
            s.stacks.clear();
            if (list != null) for (T t : list) if (t != null) s.stacks.add(t);
        }

        @Override
        public void set(int slot, T one) {
            set(slot, one == null ? List.of() : java.util.Collections.singletonList(one));
        }

        @Override
        public void setBackground(int slot, IDrawable background) {
            Slot<T> s = slots.get(slot);
            if (s != null) s.background = background;
        }

        @Override
        public void addTooltipCallback(legacyconnector.v1_12_2.mezz.jei.api.gui.ITooltipCallback<T> callback) {
            if (callback != null) tooltips.add(callback);
        }

        @Override
        public Map<Integer, ? extends legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredient<T>> getGuiIngredients() {
            return slots;
        }

        /** Item slots: an 18-pixel frame at (x, y), the item one pixel in. */
        @Override
        public void init(int slot, boolean input, int x, int y) {
            slots.put(slot, new Slot<>(input, x, y, 18, 18));
        }

        @Override
        public void setOverrideDisplayFocus(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<T> focus) {
        }
    }

    static final class ItemGroup extends Group<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> implements legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiItemStackGroup {
        ItemGroup() {
            super(legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class);
        }
    }

    static final class FluidGroup extends Group<legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack> implements legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiFluidStackGroup {
        FluidGroup() {
            super(legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.class);
        }

        @Override
        public void init(int slot, boolean input, int x, int y) {
            init(slot, input, x, y, 16, 16, 1000, false, null);
        }

        @Override
        public void init(int slot, boolean input, int x, int y, int width, int height, int capacityMb, boolean showCapacity, IDrawable overlay) {
            var s = new Slot<legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack>(input, x, y, width, height);
            s.capacity = Math.max(1, capacityMb);
            s.showCapacity = showCapacity;
            s.overlay = overlay;
            slots.put(slot, s);
        }
    }

    static final class Layout implements legacyconnector.v1_12_2.mezz.jei.api.gui.IRecipeLayout {
        final ItemGroup items = new ItemGroup();
        final FluidGroup fluids = new FluidGroup();
        boolean shapeless;

        @Override public legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiItemStackGroup getItemStacks() { return items; }
        @Override public legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiFluidStackGroup getFluidStacks() { return fluids; }

        @SuppressWarnings("unchecked")
        @Override
        public <T> legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredientGroup<T> getIngredientsGroup(IIngredientType<T> type) {
            return getIngredientsGroup((Class<T>) type.getIngredientClass());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredientGroup<T> getIngredientsGroup(Class<T> cls) {
            if (cls != null && legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.class.isAssignableFrom(cls)) return (legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredientGroup<T>) fluids;
            return (legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiIngredientGroup<T>) items;
        }

        @Override public legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<?> getFocus() { return null; }
        @Override public void setRecipeTransferButton(int posX, int posY) {}
        @Override public void setShapeless() { shapeless = true; }

        /** The legacy slots as modern ones (an item slot's item sat one pixel inside its frame). */
        void emit(IRecipeLayoutBuilder builder) {
            for (var e : items.slots.entrySet()) {
                int index = e.getKey();
                var s = e.getValue();
                IRecipeSlotBuilder slot = builder.addSlot(s.input ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, s.x + 1, s.y + 1);
                List<net.minecraft.world.item.ItemStack> stacks = new ArrayList<>();
                for (var st : s.stacks) if (st != null && !st.isEmpty()) stacks.add(Stacks1122.toModern(st));
                slot.addItemStacks(stacks);
                if (s.background != null) slot.setBackground(modern(s.background), -1, -1);
                if (!items.tooltips.isEmpty()) {
                    slot.addRichTooltipCallback((view, tooltip) -> {
                        var shown = view.getDisplayedItemStack().map(Stacks1122::fromModern).orElse(null);
                        if (shown == null) return;
                        List<String> lines = new ArrayList<>();
                        for (var cb : items.tooltips) {
                            try {
                                cb.onTooltip(index, s.input, shown, lines);
                            } catch (RuntimeException | LinkageError ex) {
                                if (FAILED.add(cb.getClass())) LOG.debug("1.12.2 JEI tooltip callback {} failed: {}", cb.getClass().getName(), ex.toString());
                            }
                        }
                        for (String l : lines) tooltip.add(Component.literal(l));
                    });
                }
            }
            for (var e : fluids.slots.entrySet()) {
                int index = e.getKey();
                var s = e.getValue();
                IRecipeSlotBuilder slot = builder.addSlot(s.input ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT, s.x, s.y);
                List<net.neoforged.neoforge.fluids.FluidStack> stacks = new ArrayList<>();
                for (var st : s.stacks) {
                    var m = st == null ? null : FluidBridge1122.toModern(st);
                    if (m != null && !m.isEmpty()) stacks.add(m);
                }
                slot.setFluidRenderer(s.capacity, s.showCapacity, s.w, s.h);
                slot.addIngredients(NeoForgeTypes.FLUID_STACK, stacks);
                if (s.overlay != null) slot.setOverlay(modern(s.overlay), 0, 0);
                if (s.background != null) slot.setBackground(modern(s.background), 0, 0);
                if (!fluids.tooltips.isEmpty()) {
                    slot.addRichTooltipCallback((view, tooltip) -> {
                        var shown = view.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK).map(FluidBridge1122::fromModern).orElse(null);
                        if (shown == null) return;
                        List<String> lines = new ArrayList<>();
                        for (var cb : fluids.tooltips) {
                            try {
                                cb.onTooltip(index, s.input, shown, lines);
                            } catch (RuntimeException | LinkageError ex) {
                                if (FAILED.add(cb.getClass())) LOG.debug("1.12.2 JEI tooltip callback {} failed: {}", cb.getClass().getName(), ex.toString());
                            }
                        }
                        for (String l : lines) tooltip.add(Component.literal(l));
                    });
                }
            }
            if (shapeless) builder.setShapeless();
        }
    }

    /** A legacy drawable where the modern JEI draws (icons, slot backgrounds and overlays), as legacy GUI drawing. */
    static mezz.jei.api.gui.drawable.IDrawable modern(IDrawable legacy) {
        return new mezz.jei.api.gui.drawable.IDrawable() {
            @Override
            public int getWidth() {
                return legacy.getWidth();
            }

            @Override
            public int getHeight() {
                return legacy.getHeight();
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                RenderContext.beginGui(graphics);
                try {
                    legacy.draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft(), x, y);
                } catch (RuntimeException | LinkageError e) {
                    if (FAILED.add(legacy.getClass())) LOG.debug("1.12.2 JEI drawable {} failed: {}", legacy.getClass().getName(), e.toString());
                } finally {
                    RenderContext.end();
                }
            }
        };
    }

    // ------------------------------------------------------------------ JEI 4's helpers

    /** A part of a texture (JEI 4's DrawableResource): padding around it, masks cutting it while it animates. */
    record TextureDrawable(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation texture, int u, int v, int width, int height,
                           int padTop, int padBottom, int padLeft, int padRight, int texWidth, int texHeight) implements IDrawableStatic {
        @Override
        public int getWidth() {
            return width + padLeft + padRight;
        }

        @Override
        public int getHeight() {
            return height + padTop + padBottom;
        }

        @Override
        public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y) {
            draw(mc, x, y, 0, 0, 0, 0);
        }

        @Override
        public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y, int maskTop, int maskBottom, int maskLeft, int maskRight) {
            int w = width - maskLeft - maskRight;
            int h = height - maskTop - maskBottom;
            if (w <= 0 || h <= 0) return;
            legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager.enableBlend();
            mc.getTextureManager().bindTexture(texture);
            legacyconnector.v1_12_2.net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x + padLeft + maskLeft, y + padTop + maskTop, u + maskLeft, v + maskTop, w, h, texWidth, texHeight);
        }
    }

    record BlankDrawable(int width, int height) implements IDrawableStatic {
        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
        @Override public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y) {}
        @Override public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y, int mt, int mb, int ml, int mr) {}
    }

    /** JEI's slot frame. */
    record SlotDrawable() implements IDrawableStatic {
        @Override public int getWidth() { return 18; }
        @Override public int getHeight() { return 18; }

        @Override
        public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y) {
            var g = RenderContext.gui();
            if (g != null) g.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"), x, y, 18, 18);
        }

        @Override
        public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y, int mt, int mb, int ml, int mr) {
            draw(mc, x, y);
        }
    }

    /** An item drawn as an icon. */
    record ItemDrawable(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) implements IDrawable {
        @Override public int getWidth() { return 16; }
        @Override public int getHeight() { return 16; }

        @Override
        public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y) {
            var g = RenderContext.gui();
            if (g != null && stack != null && !stack.isEmpty()) g.renderItem(Stacks1122.toModern(stack), x, y);
        }
    }

    /** JEI 4's TickTimer: from 0 to the maximum once per cycle (or back down), on the game's clock. */
    record TickTimer(int ticksPerCycle, int maxValue, boolean countDown) implements ITickTimer {
        @Override
        public int getValue() {
            long ticks = net.minecraft.Util.getMillis() / 50L;
            int cycle = Math.max(1, ticksPerCycle);
            int value = (int) ((ticks % cycle) * (long) (maxValue + 1) / cycle);
            value = Math.min(value, maxValue);
            return countDown ? maxValue - value : value;
        }

        @Override
        public int getMaxValue() {
            return maxValue;
        }
    }

    /** JEI 4's DrawableAnimated: the static drawable masked by the timer from the start direction. */
    record AnimatedDrawable(IDrawableStatic drawable, ITickTimer timer, IDrawableAnimated.StartDirection direction) implements IDrawableAnimated {
        @Override public int getWidth() { return drawable.getWidth(); }
        @Override public int getHeight() { return drawable.getHeight(); }

        @Override
        public void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc, int x, int y) {
            int value = timer.getValue();
            int top = 0, bottom = 0, left = 0, right = 0;
            switch (direction) {
                case TOP -> bottom = value;
                case BOTTOM -> top = value;
                case LEFT -> right = value;
                case RIGHT -> left = value;
            }
            drawable.draw(mc, x, y, top, bottom, left, right);
        }
    }

    static IDrawableAnimated animated(IDrawableStatic drawable, int ticksPerCycle, IDrawableAnimated.StartDirection direction, boolean inverted) {
        if (inverted) {
            direction = switch (direction) {
                case TOP -> IDrawableAnimated.StartDirection.BOTTOM;
                case BOTTOM -> IDrawableAnimated.StartDirection.TOP;
                case LEFT -> IDrawableAnimated.StartDirection.RIGHT;
                case RIGHT -> IDrawableAnimated.StartDirection.LEFT;
            };
        }
        boolean vertical = direction == IDrawableAnimated.StartDirection.TOP || direction == IDrawableAnimated.StartDirection.BOTTOM;
        int max = vertical ? drawable.getHeight() : drawable.getWidth();
        return new AnimatedDrawable(drawable, new TickTimer(ticksPerCycle, max, !inverted), direction);
    }

    static final class Helpers implements legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers, legacyconnector.v1_12_2.mezz.jei.api.IGuiHelper,
            legacyconnector.v1_12_2.mezz.jei.api.recipe.IStackHelper, legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientBlacklist {
        static final Helpers INSTANCE = new Helpers();
        static final List<Object> BLACKLIST = new java.util.concurrent.CopyOnWriteArrayList<>();
        static volatile mezz.jei.api.helpers.IGuiHelper modernGui;
        static volatile IJeiRuntime runtime;

        static final legacyconnector.v1_12_2.mezz.jei.api.ISubtypeRegistry SUBTYPES = new legacyconnector.v1_12_2.mezz.jei.api.ISubtypeRegistry() {
            @Override public void useNbtForSubtypes(legacyconnector.v1_12_2.net.minecraft.item.Item... items) {}
            @Override public void registerSubtypeInterpreter(legacyconnector.v1_12_2.net.minecraft.item.Item item, ISubtypeInterpreter interpreter) {}
            @Override public ISubtypeInterpreter getSubtypeInterpreter(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) { return null; }
            @Override public String getSubtypeInfo(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) { return ISubtypeInterpreter.NONE; }
            @Override public boolean hasSubtypeInterpreter(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) { return false; }
        };

        static final legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRegistry INGREDIENTS = new legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientRegistry() {
            @Override
            public <V> java.util.Collection<V> getAllIngredients(IIngredientType<V> type) {
                return getAllIngredients(type == null ? null : (Class<V>) type.getIngredientClass());
            }

            @SuppressWarnings("unchecked")
            @Override
            public <V> java.util.Collection<V> getAllIngredients(Class<V> cls) {
                List<Object> out = new ArrayList<>();
                if (cls == null) return (java.util.Collection<V>) out;
                if (legacyconnector.v1_12_2.net.minecraft.item.ItemStack.class.isAssignableFrom(cls)) {
                    for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                        if (item == net.minecraft.world.item.Items.AIR) continue;
                        out.add(Stacks1122.fromModern(new net.minecraft.world.item.ItemStack(item)));
                    }
                } else if (legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack.class.isAssignableFrom(cls)) {
                    for (var fluid : net.minecraft.core.registries.BuiltInRegistries.FLUID) {
                        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) continue;
                        var legacy = FluidBridge1122.fromModern(new net.neoforged.neoforge.fluids.FluidStack(fluid, 1000));
                        if (legacy != null) out.add(legacy);
                    }
                }
                return (java.util.Collection<V>) out;
            }

            @Override
            public List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getFuels() {
                List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new ArrayList<>();
                for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                    var stack = new net.minecraft.world.item.ItemStack(item);
                    if (!stack.isEmpty() && stack.getBurnTime(null) > 0) out.add(Stacks1122.fromModern(stack));
                }
                return out;
            }

            @Override
            public List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getPotionIngredients() {
                return new ArrayList<>();
            }

            @Override
            public boolean isFuel(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
                return stack != null && !stack.isEmpty() && Stacks1122.toModern(stack).getBurnTime(null) > 0;
            }
        };

        static final legacyconnector.v1_12_2.mezz.jei.api.IJeiRuntime RUNTIME = new legacyconnector.v1_12_2.mezz.jei.api.IJeiRuntime() {
            @Override
            public legacyconnector.v1_12_2.mezz.jei.api.IRecipeRegistry getRecipeRegistry() {
                return new legacyconnector.v1_12_2.mezz.jei.api.IRecipeRegistry() {
                    @Override
                    public <V> legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<V> createFocus(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus.Mode mode, V ingredient) {
                        return new Focus<>(ingredient, mode);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> category) {
                        List<T> out = new ArrayList<>();
                        Category c = category == null ? null : categoryOf(category.getUid());
                        if (c != null) for (Recipe r : c.recipes) out.add((T) r.wrapper());
                        return out;
                    }

                    @Override
                    public void addRecipe(IRecipeWrapper wrapper, String uid) {
                        Category c = categoryOf(uid);
                        if (c == null || wrapper == null) return;
                        Recipe r = new Recipe(wrapper, c);
                        c.recipes.add(r);
                        try {
                            if (runtime != null) runtime.getRecipeManager().addRecipes(c.type, List.of(r));
                        } catch (RuntimeException e) {
                            LOG.debug("JEI refused a 1.12.2 recipe added at runtime to {}: {}", uid, e.toString());
                        }
                    }

                    @Override
                    public void removeRecipe(IRecipeWrapper wrapper, String uid) {
                        Category c = categoryOf(uid);
                        if (c == null || wrapper == null) return;
                        List<Recipe> gone = new ArrayList<>();
                        for (Recipe r : c.recipes) if (r.wrapper() == wrapper) gone.add(r);
                        if (gone.isEmpty()) return;
                        c.recipes.removeAll(gone);
                        try {
                            if (runtime != null) runtime.getRecipeManager().hideRecipes(c.type, gone);
                        } catch (RuntimeException e) {
                            LOG.debug("JEI refused to hide a 1.12.2 recipe of {}: {}", uid, e.toString());
                        }
                    }
                };
            }

            @Override
            public legacyconnector.v1_12_2.mezz.jei.api.IRecipesGui getRecipesGui() {
                return new legacyconnector.v1_12_2.mezz.jei.api.IRecipesGui() {
                    @Override
                    public <V> void show(legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<V> focus) {
                        var rt = runtime;
                        if (rt == null || focus == null) return;
                        var role = focus.getMode() == legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus.Mode.INPUT ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
                        Object v = focus.getValue();
                        var factory = rt.getJeiHelpers().getFocusFactory();
                        if (v instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s) {
                            rt.getRecipesGui().show(factory.createFocus(role, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, Stacks1122.toModern(s)));
                        } else if (v instanceof legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack f) {
                            rt.getRecipesGui().show(factory.createFocus(role, NeoForgeTypes.FLUID_STACK, FluidBridge1122.toModern(f)));
                        }
                    }

                    @Override
                    public void showCategories(List<String> uids) {
                    }
                };
            }

            @Override
            public legacyconnector.v1_12_2.mezz.jei.api.IIngredientFilter getIngredientFilter() {
                return new legacyconnector.v1_12_2.mezz.jei.api.IIngredientFilter() {
                    @Override
                    public void setFilterText(String text) {
                        if (runtime != null) runtime.getIngredientFilter().setFilterText(text == null ? "" : text);
                    }

                    @Override
                    public String getFilterText() {
                        return runtime == null ? "" : runtime.getIngredientFilter().getFilterText();
                    }
                };
            }
        };

        /** The bridged category of a legacy uid. */
        static Category categoryOf(String uid) {
            var bridge = current;
            if (bridge == null || uid == null) return null;
            for (Category c : bridge.categories) if (c.uid.equals(uid)) return c;
            return null;
        }

        record Focus<V>(V value, legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus.Mode mode) implements legacyconnector.v1_12_2.mezz.jei.api.recipe.IFocus<V> {
            @Override public V getValue() { return value; }
            @Override public Mode getMode() { return mode; }
        }

        // IJeiHelpers
        @Override public legacyconnector.v1_12_2.mezz.jei.api.IGuiHelper getGuiHelper() { return this; }
        @Override public legacyconnector.v1_12_2.mezz.jei.api.recipe.IStackHelper getStackHelper() { return this; }
        @Override public legacyconnector.v1_12_2.mezz.jei.api.ingredients.IIngredientBlacklist getIngredientBlacklist() { return this; }
        @Override public legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper getRecipeTransferHandlerHelper() { return new legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper() {}; }

        // IGuiHelper
        @Override
        public legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableBuilder drawableBuilder(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation rl, int u, int v, int w, int h) {
            return new Builder(rl, u, v, w, h);
        }

        @Override
        public IDrawableStatic createDrawable(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation rl, int u, int v, int w, int h) {
            return new TextureDrawable(rl, u, v, w, h, 0, 0, 0, 0, 256, 256);
        }

        @Override
        public IDrawableStatic createDrawable(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation rl, int u, int v, int w, int h, int pt, int pb, int pl, int pr) {
            return new TextureDrawable(rl, u, v, w, h, pt, pb, pl, pr, 256, 256);
        }

        @Override
        public IDrawableAnimated createAnimatedDrawable(IDrawableStatic drawable, int ticksPerCycle, IDrawableAnimated.StartDirection direction, boolean inverted) {
            return animated(drawable, ticksPerCycle, direction, inverted);
        }

        @Override public IDrawableStatic getSlotDrawable() { return new SlotDrawable(); }
        @Override public IDrawableStatic createBlankDrawable(int w, int h) { return new BlankDrawable(w, h); }

        @Override
        public <V> IDrawable createDrawableIngredient(V ingredient) {
            return ingredient instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s ? new ItemDrawable(s) : new BlankDrawable(16, 16);
        }

        @Override
        public legacyconnector.v1_12_2.mezz.jei.api.gui.ICraftingGridHelper createCraftingGridHelper(int firstInput, int output) {
            return new legacyconnector.v1_12_2.mezz.jei.api.gui.ICraftingGridHelper() {
                @Override
                public void setInputs(legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiItemStackGroup group, List<List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> input) {
                    int size = input == null ? 0 : input.size();
                    int side = size > 4 ? 3 : size > 1 ? 2 : 1;
                    setInputs(group, input, side, side);
                }

                @Override
                public void setInputs(legacyconnector.v1_12_2.mezz.jei.api.gui.IGuiItemStackGroup group, List<List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> input, int width, int height) {
                    if (input == null) return;
                    for (int i = 0; i < input.size(); i++) group.set(firstInput + craftingIndex(i, width, height), input.get(i));
                }
            };
        }

        /** JEI 4's placement of a recipe smaller than 3x3 in the grid. */
        static int craftingIndex(int i, int width, int height) {
            if (width == 1) return height == 3 || height == 2 ? i * 3 + 1 : 4;
            if (height == 1) return i + 3;
            if (width == 2) {
                int index = i;
                if (i > 1) {
                    index++;
                    if (i > 3) index++;
                }
                return index;
            }
            return height == 2 ? i + 3 : i;
        }

        @Override
        public ITickTimer createTickTimer(int ticksPerCycle, int maxValue, boolean countDown) {
            return new TickTimer(ticksPerCycle, maxValue, countDown);
        }

        // IStackHelper
        @Override
        public List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getSubtypes(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
            List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new ArrayList<>();
            if (stack != null && !stack.isEmpty()) out.add(stack);
            return out;
        }

        @Override
        public List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> getAllSubtypes(Iterable<?> stacks) {
            List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new ArrayList<>();
            if (stacks != null) for (Object o : stacks) out.addAll(toItemStackList(o));
            return out;
        }

        @Override
        public List<List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> expandRecipeItemStackInputs(List<?> inputs) {
            List<List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack>> out = new ArrayList<>();
            if (inputs != null) for (Object o : inputs) out.add(toItemStackList(o));
            return out;
        }

        @Override
        public List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> toItemStackList(Object o) {
            List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> out = new ArrayList<>();
            if (o instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemStack s) {
                if (!s.isEmpty()) out.add(s);
            } else if (o instanceof legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient i) {
                for (var s : i.getMatchingStacks()) if (s != null && !s.isEmpty()) out.add(s);
            } else if (o instanceof String ore) {
                out.addAll(legacyconnector.v1_12_2.net.minecraftforge.oredict.OreDictionary.getOres(ore));
            } else if (o instanceof Iterable<?> it) {
                for (Object e : it) out.addAll(toItemStackList(e));
            } else if (o instanceof Object[] array) {
                for (Object e : array) out.addAll(toItemStackList(e));
            }
            return out;
        }

        @Override
        public boolean isEquivalent(legacyconnector.v1_12_2.net.minecraft.item.ItemStack a, legacyconnector.v1_12_2.net.minecraft.item.ItemStack b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            return a.isItemEqual(b) && legacyconnector.v1_12_2.net.minecraft.item.ItemStack.areItemStackTagsEqual(a, b);
        }

        @Override
        public String getUniqueIdentifierForStack(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
            if (stack == null || stack.isEmpty()) return "";
            var name = stack.getItem().getRegistryName();
            return (name == null ? "" : name.toString()) + ":" + stack.getMetadata();
        }

        // IIngredientBlacklist
        @Override public <V> void addIngredientToBlacklist(V ingredient) { if (ingredient != null) BLACKLIST.add(ingredient); }
        @Override public <V> void removeIngredientFromBlacklist(V ingredient) { BLACKLIST.remove(ingredient); }
        @Override public <V> boolean isIngredientBlacklisted(V ingredient) { return BLACKLIST.contains(ingredient); }
    }

    /** JEI 4's DrawableBuilder. */
    static final class Builder implements legacyconnector.v1_12_2.mezz.jei.api.gui.IDrawableBuilder {
        private final legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation texture;
        private int u, v, w, h, texW = 256, texH = 256, pt, pb, pl, pr;

        Builder(legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation texture, int u, int v, int w, int h) {
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
        }

        @Override public Builder setTextureSize(int width, int height) { texW = width; texH = height; return this; }
        @Override public Builder addPadding(int top, int bottom, int left, int right) { pt = top; pb = bottom; pl = left; pr = right; return this; }

        @Override
        public Builder trim(int top, int bottom, int left, int right) {
            u += left;
            v += top;
            w -= left + right;
            h -= top + bottom;
            return this;
        }

        @Override
        public IDrawableStatic build() {
            return new TextureDrawable(texture, u, v, w, h, pt, pb, pl, pr, texW, texH);
        }

        @Override
        public IDrawableAnimated buildAnimated(int ticksPerCycle, IDrawableAnimated.StartDirection direction, boolean inverted) {
            return animated(build(), ticksPerCycle, direction, inverted);
        }

        @Override
        public IDrawableAnimated buildAnimated(ITickTimer timer, IDrawableAnimated.StartDirection direction) {
            return new AnimatedDrawable(build(), timer, direction);
        }
    }
}
