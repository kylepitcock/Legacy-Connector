package legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButtonToggle;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe;
import legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 recipe book page: twenty recipe buttons in a 5x4 grid with page arrows; a left click remembers the clicked recipe for the book to
 * place. (1.12's right-click overlay of a button's alternative recipes is not drawn; a left click takes the recipe shown.)
 */
public class RecipeBookPage {
    private static final ResourceLocation RECIPE_BOOK = new ResourceLocation("textures/gui/recipe_book.png");
    private final List<GuiButtonRecipe> buttons = new ArrayList<>(20);
    private GuiButtonRecipe hoveredButton;
    private Minecraft minecraft;
    private final List<GuiRecipeBook> listeners = new ArrayList<>();
    private List<RecipeList> recipeLists = new ArrayList<>();
    private GuiButtonToggle forwardButton;
    private GuiButtonToggle backButton;
    private int totalPages;
    private int currentPage;
    private RecipeBook recipeBook;
    private IRecipe lastClickedRecipe;
    private RecipeList lastClickedRecipeList;

    public RecipeBookPage() {
        for (int i = 0; i < 20; ++i) buttons.add(new GuiButtonRecipe());
    }

    public void init(Minecraft mc, int x, int y) {
        this.minecraft = mc;
        var player = Minecraft.mcc$get$player(mc);
        this.recipeBook = player == null ? new RecipeBook() : player.getRecipeBook();
        for (int i = 0; i < buttons.size(); ++i) buttons.get(i).setPosition(x + 11 + 25 * (i % 5), y + 31 + 25 * (i / 5));
        forwardButton = new GuiButtonToggle(0, x + 93, y + 137, 12, 17, false);
        forwardButton.initTextureValues(1, 208, 13, 18, RECIPE_BOOK);
        backButton = new GuiButtonToggle(0, x + 38, y + 137, 12, 17, true);
        backButton.initTextureValues(1, 208, 13, 18, RECIPE_BOOK);
    }

    public void addListener(GuiRecipeBook listener) {
        listeners.remove(listener);
        listeners.add(listener);
    }

    public void updateLists(List<RecipeList> lists, boolean resetPage) {
        this.recipeLists = lists;
        this.totalPages = (int) Math.ceil((double) lists.size() / 20.0D);
        if (totalPages <= currentPage || resetPage) currentPage = 0;
        updateButtonsForPage();
    }

    private void updateButtonsForPage() {
        int first = 20 * currentPage;
        for (int j = 0; j < buttons.size(); ++j) {
            GuiButtonRecipe button = buttons.get(j);
            if (first + j < recipeLists.size() && recipeBook != null) {
                button.init(recipeLists.get(first + j), this, recipeBook);
                button.visible = true;
            } else {
                button.visible = false;
            }
        }
        updateArrowButtons();
    }

    private void updateArrowButtons() {
        if (forwardButton == null || backButton == null) return;
        forwardButton.visible = totalPages > 1 && currentPage < totalPages - 1;
        backButton.visible = totalPages > 1 && currentPage > 0;
    }

    public void render(int x, int y, int mouseX, int mouseY, float partialTicks) {
        if (minecraft == null) return;
        if (totalPages > 1) {
            String s = (currentPage + 1) + "/" + totalPages;
            int w = minecraft.fontRenderer.getStringWidth(s);
            minecraft.fontRenderer.drawString(s, x - w / 2 + 73, y + 141, -1);
        }
        hoveredButton = null;
        for (GuiButtonRecipe button : buttons) {
            button.drawButton(minecraft, mouseX, mouseY, partialTicks);
            if (button.visible && button.isMouseOver()) hoveredButton = button;
        }
        if (backButton != null) backButton.drawButton(minecraft, mouseX, mouseY, partialTicks);
        if (forwardButton != null) forwardButton.drawButton(minecraft, mouseX, mouseY, partialTicks);
    }

    public void renderTooltip(int x, int y) {
        if (minecraft == null || hoveredButton == null) return;
        var screen = Minecraft.mcc$get$currentScreen(minecraft);
        if (screen != null) screen.drawHoveringText(hoveredButton.getToolTipText(screen), x, y);
    }

    public IRecipe getLastClickedRecipe() {
        return lastClickedRecipe;
    }

    public RecipeList getLastClickedRecipeList() {
        return lastClickedRecipeList;
    }

    public void setInvisible() {
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int x, int y, int width, int height) {
        lastClickedRecipe = null;
        lastClickedRecipeList = null;
        if (minecraft == null) return false;
        if (forwardButton != null && mouseButton == 0 && forwardButton.mousePressed(minecraft, mouseX, mouseY)) {
            forwardButton.playPressSound(minecraft.getSoundHandler());
            ++currentPage;
            updateButtonsForPage();
            return true;
        }
        if (backButton != null && mouseButton == 0 && backButton.mousePressed(minecraft, mouseX, mouseY)) {
            backButton.playPressSound(minecraft.getSoundHandler());
            --currentPage;
            updateButtonsForPage();
            return true;
        }
        for (GuiButtonRecipe button : buttons) {
            if (button.mousePressed(minecraft, mouseX, mouseY)) {
                button.playPressSound(minecraft.getSoundHandler());
                if (mouseButton == 0) {
                    lastClickedRecipe = button.getRecipe();
                    lastClickedRecipeList = button.getList();
                }
                return true;
            }
        }
        return false;
    }

    public void recipesShown(List<IRecipe> recipes) {
        for (GuiRecipeBook listener : listeners) listener.recipesShown(recipes);
    }
}
