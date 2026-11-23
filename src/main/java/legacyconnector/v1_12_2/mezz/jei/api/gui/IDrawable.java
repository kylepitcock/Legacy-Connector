package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: something drawn in a recipe. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IDrawable {
    int getWidth();
    int getHeight();
    default void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft) {
        draw(minecraft, 0, 0);
    }
    void draw(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, int xOffset, int yOffset);
}
