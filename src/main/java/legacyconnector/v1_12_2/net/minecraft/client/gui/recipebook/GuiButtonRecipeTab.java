package legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButtonToggle;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;

/**
 * A category tab on the side of the recipe book. The modern book draws its own tabs; these carry their position and visibility for
 * legacy code that keeps out of the book's area (JEI's exclusion zones).
 */
public class GuiButtonRecipeTab extends GuiButtonToggle {
    private final CreativeTabs category;
    private float animationTime;

    public GuiButtonRecipeTab(int id, CreativeTabs categoryIn) {
        super(id, 0, 0, 35, 27, false);
        this.category = categoryIn;
    }

    public void startAnimation(Minecraft mcIn) {
        animationTime = 15.0F;
    }

    public CreativeTabs getCategory() {
        return category;
    }

    public boolean updateVisibility() {
        return visible;
    }

    /** Drawn by the modern recipe book. */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (animationTime > 0.0F) animationTime = Math.max(0.0F, animationTime - partialTicks);
    }
}
