package legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.12.2 recipe book button: one recipe list, cycling through its recipes' results every 30 ticks; red when nothing can be crafted. */
public class GuiButtonRecipe extends GuiButton {
    private static final ResourceLocation RECIPE_BOOK = new ResourceLocation("textures/gui/recipe_book.png");
    private RecipeBook book;
    private RecipeList list;
    private float time;
    private float animationTime;
    private int currentIndex;

    public GuiButtonRecipe() {
        super(0, 0, 0, 25, 25, "");
    }

    public void init(RecipeList listIn, RecipeBookPage page, RecipeBook bookIn) {
        this.list = listIn;
        this.book = bookIn;
        List<IRecipe> recipes = listIn.getRecipes(bookIn.isFilteringCraftable());
        for (IRecipe recipe : recipes) {
            if (bookIn.isNew(recipe)) {
                page.recipesShown(recipes);
                animationTime = 15.0F;
                break;
            }
        }
    }

    public RecipeList getList() {
        return list;
    }

    public void setPosition(int xIn, int yIn) {
        this.x = xIn;
        this.y = yIn;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible || list == null || book == null) return;
        if (!GuiScreen.isCtrlKeyDown()) time += partialTicks;
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        RenderContext.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(RECIPE_BOOK);
        int u = 29;
        if (!list.containsCraftableRecipes()) u += 25;
        int v = 206;
        if (list.getRecipes(book.isFilteringCraftable()).size() > 1) v += 25;
        if (animationTime > 0.0F) animationTime = Math.max(0.0F, animationTime - partialTicks);
        drawTexturedModalRect(x, y, u, v, width, height);
        List<IRecipe> recipes = getOrderedRecipes();
        if (recipes.isEmpty()) return;
        currentIndex = (int) Math.floor(time / 30.0F) % recipes.size();
        ItemStack stack = recipes.get(currentIndex).getRecipeOutput();
        int offset = 4;
        var renderItem = mc.getRenderItem();
        if (list.hasSingleResultItem() && recipes.size() > 1) {
            renderItem.renderItemAndEffectIntoGUI(stack, x + offset + 1, y + offset + 1);
            --offset;
        }
        renderItem.renderItemAndEffectIntoGUI(stack, x + offset, y + offset);
    }

    private List<IRecipe> getOrderedRecipes() {
        List<IRecipe> recipes = list.getDisplayRecipes(true);
        if (!book.isFilteringCraftable()) recipes.addAll(list.getDisplayRecipes(false));
        return recipes;
    }

    public boolean isOnlyOption() {
        return getOrderedRecipes().size() == 1;
    }

    public IRecipe getRecipe() {
        List<IRecipe> recipes = getOrderedRecipes();
        return recipes.isEmpty() ? null : recipes.get(Math.min(currentIndex, recipes.size() - 1));
    }

    public List<String> getToolTipText(GuiScreen screen) {
        IRecipe recipe = getRecipe();
        List<String> lines = recipe == null ? new java.util.ArrayList<>() : screen.getItemToolTip(recipe.getRecipeOutput());
        if (list.getRecipes(book.isFilteringCraftable()).size() > 1) lines.add(I18n.format("gui.recipebook.moreRecipes"));
        return lines;
    }

    @Override
    public int getButtonWidth() {
        return 25;
    }
}
