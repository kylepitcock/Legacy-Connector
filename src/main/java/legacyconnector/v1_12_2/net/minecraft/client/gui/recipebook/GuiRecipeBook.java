package legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.Gui;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's recipe book panel, as the modern {@link RecipeBookComponent}. The game's own inventory and crafting screens lend theirs (the
 * legacy stand-ins of those screens); a legacy GUI that builds its own book gets a modern one when the player's open menu has a recipe
 * book (the inventory, a crafting table), otherwise the book stays closed. Position fields and the tab buttons follow the modern panel.
 */
public class GuiRecipeBook extends Gui implements IRecipeUpdateListener {
    protected static final ResourceLocation RECIPE_BOOK = new ResourceLocation("textures/gui/recipe_book.png");
    protected Minecraft mc;
    private int xOffset;
    private int width;
    private int height;
    private final List<GuiButtonRecipeTab> recipeTabs = new ArrayList<>();
    private RecipeBookComponent mcc$book;
    private boolean mcc$borrowed;
    private boolean mcc$widthTooNarrow;

    public GuiRecipeBook() {
    }

    /** The legacy face of a modern screen's recipe book. */
    public static GuiRecipeBook mcc$of(RecipeBookComponent book) {
        GuiRecipeBook out = new GuiRecipeBook();
        out.mcc$book = book;
        out.mcc$borrowed = true;
        out.mc = Minecraft.getMinecraft();
        out.mcc$refresh();
        return out;
    }

    public RecipeBookComponent mcc$modern() {
        return mcc$book;
    }

    /** Reads the panel's position and tabs back from the modern book. */
    private void mcc$refresh() {
        if (mcc$book == null) return;
        try {
            var cls = RecipeBookComponent.class;
            var fx = cls.getDeclaredField("xOffset");
            var fw = cls.getDeclaredField("width");
            var fh = cls.getDeclaredField("height");
            var ft = cls.getDeclaredField("tabButtons");
            fx.setAccessible(true);
            fw.setAccessible(true);
            fh.setAccessible(true);
            ft.setAccessible(true);
            xOffset = fx.getInt(mcc$book);
            width = fw.getInt(mcc$book);
            height = fh.getInt(mcc$book);
            List<?> tabs = (List<?>) ft.get(mcc$book);
            for (int i = 0; i < tabs.size(); i++) {
                var modern = (net.minecraft.client.gui.components.AbstractWidget) tabs.get(i);
                if (i >= recipeTabs.size()) recipeTabs.add(new GuiButtonRecipeTab(i, null));
                GuiButtonRecipeTab tab = recipeTabs.get(i);
                tab.x = modern.getX();
                tab.y = modern.getY();
                tab.width = modern.getWidth();
                tab.height = modern.getHeight();
                tab.visible = modern.visible;
            }
            while (recipeTabs.size() > tabs.size()) recipeTabs.remove(recipeTabs.size() - 1);
        } catch (ReflectiveOperationException | RuntimeException e) {
            xOffset = mcc$widthTooNarrow ? 0 : 86;
        }
    }

    /** Sets the book up for a screen of the given size; 1.12 passed the crafting grid, the modern book reads the open menu. */
    public void func_194303_a(int widthIn, int heightIn, Minecraft mcIn, boolean widthTooNarrowIn, InventoryCrafting craftingSlotsIn) {
        this.mc = mcIn;
        this.width = widthIn;
        this.height = heightIn;
        this.mcc$widthTooNarrow = widthTooNarrowIn;
        if (mcc$borrowed) {
            mcc$refresh();
            return;
        }
        var modern = net.minecraft.client.Minecraft.getInstance();
        // RecipeBookComponent.init makes its menu the player's open menu: only bind one the player already has open
        if (modern.player != null && modern.player.containerMenu instanceof net.minecraft.world.inventory.RecipeBookMenu<?, ?> menu) {
            if (mcc$book == null) mcc$book = new RecipeBookComponent();
            mcc$book.init(widthIn, heightIn, modern, widthTooNarrowIn, menu);
            mcc$refresh();
        } else {
            mcc$book = null;
        }
    }

    public void initVisuals(boolean widthTooNarrowIn, InventoryCrafting craftingSlotsIn) {
        this.mcc$widthTooNarrow = widthTooNarrowIn;
        if (mcc$book != null) {
            mcc$book.initVisuals();
            mcc$refresh();
        }
    }

    /** The modern book keeps no state to release when its screen closes. */
    public void removed() {
    }

    /** The GUI's left edge: moved right of the book when the book is open and the screen is wide enough. */
    public int updateScreenPosition(boolean widthTooNarrowIn, int screenWidth, int xSize) {
        if (mcc$book != null) return mcc$book.updateScreenPosition(screenWidth, xSize);
        return (screenWidth - xSize) / 2;
    }

    public void toggleVisibility() {
        if (mcc$book != null) {
            mcc$book.toggleVisibility();
            mcc$refresh();
        }
    }

    public boolean isVisible() {
        return mcc$book != null && mcc$book.isVisible();
    }

    /** A slot of the crafting grid changed: the ghost recipe is cleared (the modern book finds the slot by its number). */
    public void slotClicked(Slot slotIn) {
        if (mcc$book == null || slotIn == null) return;
        var menu = net.minecraft.client.Minecraft.getInstance().player == null ? null : net.minecraft.client.Minecraft.getInstance().player.containerMenu;
        if (menu != null && slotIn.slotNumber >= 0 && slotIn.slotNumber < menu.slots.size()) mcc$book.slotClicked(menu.slots.get(slotIn.slotNumber));
    }

    public void tick() {
        if (mcc$book != null && !mcc$borrowed) mcc$book.tick();
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        var gui = RenderContext.gui();
        if (gui == null || mcc$book == null || mcc$borrowed) return;
        mcc$book.render(gui, mouseX, mouseY, partialTicks);
        mcc$refresh();
    }

    public void renderTooltip(int guiLeft, int guiTop, int mouseX, int mouseY) {
        var gui = RenderContext.gui();
        if (gui != null && mcc$book != null && !mcc$borrowed) mcc$book.renderTooltip(gui, guiLeft, guiTop, mouseX, mouseY);
    }

    public void renderGhostRecipe(int guiLeft, int guiTop, boolean bigResultSlot, float partialTicks) {
        var gui = RenderContext.gui();
        if (gui != null && mcc$book != null && !mcc$borrowed) mcc$book.renderGhostRecipe(gui, guiLeft, guiTop, bigResultSlot, partialTicks);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return mcc$book != null && !mcc$borrowed && mcc$book.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean hasClickedOutside(int mouseX, int mouseY, int guiLeft, int guiTop, int xSize, int ySize) {
        if (mcc$book == null) return true;
        return mcc$book.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, xSize, ySize, 0);
    }

    /** A legacy key (LWJGL 2 code) or typed character for the search field and the book's keys. */
    public boolean keyPressed(char typedChar, int keyCode) {
        if (mcc$book == null || mcc$borrowed) return false;
        int glfw = keyCode > 0 ? KeyCodes.toGlfw(keyCode) : -1;
        if (glfw > 0 && mcc$book.keyPressed(glfw, 0, 0)) return true;
        return typedChar >= ' ' && mcc$book.charTyped(typedChar, 0);
    }

    public void recipesUpdated() {
        if (mcc$book != null) mcc$book.recipesUpdated();
    }

    /** Recipes the book shows are the modern book's own (its unlock toasts and pages update by themselves). */
    @Override
    public void recipesShown(List<?> recipes) {
    }
}
