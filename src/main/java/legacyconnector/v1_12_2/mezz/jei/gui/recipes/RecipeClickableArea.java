package legacyconnector.v1_12_2.mezz.jei.gui.recipes;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public class RecipeClickableArea {
    private final int x, y, width, height;
    private final java.util.List<String> recipeCategoryUids;

    public RecipeClickableArea(int top, int bottom, int left, int right, String... recipeCategoryUids) {
        this.y = top;
        this.height = bottom - top;
        this.x = left;
        this.width = right - left;
        this.recipeCategoryUids = java.util.List.of(recipeCategoryUids);
    }

    public boolean checkHover(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public java.util.List<String> getRecipeCategoryUids() {
        return recipeCategoryUids;
    }
}
