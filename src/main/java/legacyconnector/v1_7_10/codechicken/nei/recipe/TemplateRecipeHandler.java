package legacyconnector.v1_7_10.codechicken.nei.recipe;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.codechicken.nei.NEIServerUtils;
import legacyconnector.v1_7_10.codechicken.nei.PositionedStack;
import legacyconnector.v1_7_10.codechicken.nei.api.IOverlayHandler;
import legacyconnector.v1_7_10.codechicken.nei.api.IRecipeOverlayRenderer;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * NEI's base recipe handler, behaving as in NEI 1.0.5: subclasses fill {@link #arecipes} from
 * {@code loadCraftingRecipes}/{@code loadUsageRecipes}. Under Legacy Connector the recipes are shown in JEI, which calls
 * {@link #drawBackground}/{@link #drawForeground} inside the recipe area with NEI's origin (the handler's 166-wide area, where
 * its {@link PositionedStack}s are placed), so the machine picture, gauges and text a handler draws appear as in NEI.
 */
public abstract class TemplateRecipeHandler implements ICraftingHandler, IUsageHandler {
    public int cycleticks;
    public ArrayList<CachedRecipe> arecipes = new ArrayList<>();
    public LinkedList<RecipeTransferRect> transferRects = new LinkedList<>();

    public TemplateRecipeHandler() {
        loadTransferRects();
    }

    public void loadTransferRects() {
    }

    public void loadCraftingRecipes(String outputId, Object... results) {
        if ("item".equals(outputId) && results.length > 0 && results[0] instanceof ItemStack s) loadCraftingRecipes(s);
    }

    public void loadCraftingRecipes(ItemStack result) {
    }

    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if ("item".equals(inputId) && ingredients.length > 0 && ingredients[0] instanceof ItemStack s) loadUsageRecipes(s);
    }

    public void loadUsageRecipes(ItemStack ingredient) {
    }

    public abstract String getGuiTexture();

    public String getOverlayIdentifier() {
        return null;
    }

    public void drawExtras(int recipe) {
    }

    /** NEI: a bar filling over {@code ticks} ticks, restarting (driven by {@link #cycleticks}). */
    public void drawProgressBar(int x, int y, int tx, int ty, int w, int h, int ticks, int direction) {
        drawProgressBar(x, y, tx, ty, w, h, ticks <= 0 ? 0.0F : (cycleticks % ticks) / (float) ticks, direction);
    }

    /**
     * NEI: draws the part {@code completion} of the texture region (tx, ty, w, h) at (x, y), growing right (0), down (1), left (2)
     * or up (3); directions 4-7 are the same directions emptying instead of filling.
     */
    public void drawProgressBar(int x, int y, int tx, int ty, int w, int h, float completion, int direction) {
        if (direction > 3) {
            completion = 1.0F - completion;
            direction %= 4;
        }
        int var = (int) (completion * (direction % 2 == 0 ? w : h));
        switch (direction) {
            case 0 -> mcc$drawTexturedModalRect(x, y, tx, ty, var, h);
            case 1 -> mcc$drawTexturedModalRect(x, y, tx, ty, w, var);
            case 2 -> mcc$drawTexturedModalRect(x + w - var, y, tx + w - var, ty, var, h);
            case 3 -> mcc$drawTexturedModalRect(x, y + h - var, tx, ty + h - var, w, var);
            default -> {
            }
        }
    }

    /** NEI's {@code GuiDraw.changeTexture(String)}: {@code new ResourceLocation(path)} bound through the texture manager. */
    public static void mcc$changeTexture(String texture) {
        if (texture == null || texture.isEmpty()) return;
        legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().getTextureManager()
                .bindTexture(MCC$TEXTURES.computeIfAbsent(texture, legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation::new));
    }

    private static final java.util.Map<String, legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation> MCC$TEXTURES =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final legacyconnector.v1_7_10.net.minecraft.client.gui.Gui MCC$GUI = new legacyconnector.v1_7_10.net.minecraft.client.gui.Gui();

    /** NEI's {@code GuiDraw.drawTexturedModalRect}: a region of the bound 256x256 texture. */
    public static void mcc$drawTexturedModalRect(int x, int y, int u, int v, int w, int h) {
        if (w <= 0 || h <= 0) return;
        MCC$GUI.drawTexturedModalRect(x, y, u, v, w, h);
    }

    public List<Class<? extends GuiContainer>> getRecipeTransferRectGuis() {
        Class<? extends GuiContainer> gui = getGuiClass();
        List<Class<? extends GuiContainer>> out = new ArrayList<>();
        if (gui != null) out.add(gui);
        return out;
    }

    public Class<? extends GuiContainer> getGuiClass() {
        return null;
    }

    public TemplateRecipeHandler newInstance() {
        try {
            var ctor = getClass().getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot create " + getClass().getName(), e);
        }
    }

    @Override
    public ICraftingHandler getRecipeHandler(String outputId, Object... results) {
        TemplateRecipeHandler h = newInstance();
        h.loadCraftingRecipes(outputId, results);
        return h;
    }

    @Override
    public IUsageHandler getUsageHandler(String inputId, Object... ingredients) {
        TemplateRecipeHandler h = newInstance();
        h.loadUsageRecipes(inputId, ingredients);
        return h;
    }

    @Override
    public int numRecipes() {
        return arecipes.size();
    }

    /** NEI: the region (5, 11, 166x65) of the handler's GUI texture at the origin. */
    @Override
    public void drawBackground(int recipe) {
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        String texture = getGuiTexture();
        if (texture == null) return;
        mcc$changeTexture(texture);
        mcc$drawTexturedModalRect(0, 0, 5, 11, 166, 65);
    }

    /** NEI: the GUI texture bound again (progress bars are cut from it), then {@link #drawExtras}. */
    @Override
    public void drawForeground(int recipe) {
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glDisable(2896); // GL_LIGHTING
        mcc$changeTexture(getGuiTexture());
        drawExtras(recipe);
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        return arecipes.get(recipe).getIngredients();
    }

    @Override
    public PositionedStack getResultStack(int recipe) {
        return arecipes.get(recipe).getResult();
    }

    @Override
    public List<PositionedStack> getOtherStacks(int recipe) {
        return arecipes.get(recipe).getOtherStacks();
    }

    @Override
    public void onUpdate() {
        cycleticks++;
    }

    @Override
    public boolean hasOverlay(GuiContainer gui, Container container, int recipe) {
        return false;
    }

    @Override
    public IRecipeOverlayRenderer getOverlayRenderer(GuiContainer gui, int recipe) {
        return null;
    }

    @Override
    public IOverlayHandler getOverlayHandler(GuiContainer gui, int recipe) {
        return null;
    }

    @Override
    public int recipiesPerPage() {
        return 2;
    }

    @Override
    public List<String> handleTooltip(GuiRecipe gui, List<String> tip, int recipe) {
        return tip;
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe gui, ItemStack stack, List<String> tip, int recipe) {
        return tip;
    }

    @Override
    public boolean keyTyped(GuiRecipe gui, char c, int key, int recipe) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiRecipe gui, int button, int recipe) {
        return false;
    }

    /** One recipe of the handler: result, ingredients and other stacks (fuel, catalysts) with their positions. */
    public abstract class CachedRecipe {
        final long offset = System.currentTimeMillis();

        public CachedRecipe() {
        }

        public abstract PositionedStack getResult();

        public List<PositionedStack> getIngredients() {
            List<PositionedStack> out = new ArrayList<>();
            PositionedStack s = getIngredient();
            if (s != null) out.add(s);
            return out;
        }

        public PositionedStack getIngredient() {
            return null;
        }

        public List<PositionedStack> getOtherStacks() {
            List<PositionedStack> out = new ArrayList<>();
            PositionedStack s = getOtherStack();
            if (s != null) out.add(s);
            return out;
        }

        public PositionedStack getOtherStack() {
            return null;
        }

        public List<PositionedStack> getCycledIngredients(int cycle, List<PositionedStack> ingredients) {
            for (int i = 0; i < ingredients.size(); i++) randomRenderPermutation(ingredients.get(i), cycle + i);
            return ingredients;
        }

        public void randomRenderPermutation(PositionedStack stack, long cycle) {
            Random rand = new Random(cycle + offset);
            stack.setPermutationToRender(Math.abs(rand.nextInt()) % Math.max(1, stack.items.length));
        }

        public void setIngredientPermutation(Collection<PositionedStack> ingredients, ItemStack ingredient) {
            for (PositionedStack stack : ingredients) {
                for (int i = 0; i < stack.items.length; i++) {
                    if (NEIServerUtils.areStacksSameTypeCrafting(ingredient, stack.items[i])) {
                        stack.item = stack.items[i].copy();
                        break;
                    }
                }
            }
        }

        public boolean contains(Collection<PositionedStack> ingredients, ItemStack ingredient) {
            for (PositionedStack stack : ingredients) if (stack.contains(ingredient)) return true;
            return false;
        }

        public boolean contains(Collection<PositionedStack> ingredients, Item ingredient) {
            for (PositionedStack stack : ingredients) if (stack.contains(ingredient)) return true;
            return false;
        }
    }

    /** Clickable area in a machine GUI that opens this handler's recipes. */
    public static class RecipeTransferRect {
        Rectangle rect;
        String outputId;
        Object[] results;

        public RecipeTransferRect(Rectangle rect, String outputId, Object... results) {
            this.rect = rect;
            this.outputId = outputId;
            this.results = results;
        }

        public String mcc$outputId() {
            return outputId;
        }

        public Object[] mcc$results() {
            return results;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RecipeTransferRect r && rect.equals(r.rect);
        }

        @Override
        public int hashCode() {
            return rect.hashCode();
        }
    }
}
