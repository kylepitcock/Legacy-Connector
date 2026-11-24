package legacyconnector.v1_12_2.mezz.jei.api.ingredients;

/** JEI 4 API: draws an ingredient of a mod's own type (recorded; slots of such types are not shown by the modern JEI). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredientRenderer<T> {
    void render(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, int xPosition, int yPosition, T ingredient);
    java.util.List<String> getTooltip(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, T ingredient, legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag tooltipFlag);
    default legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer getFontRenderer(legacyconnector.v1_12_2.net.minecraft.client.Minecraft minecraft, T ingredient) {
        return minecraft.fontRenderer;
    }
}
