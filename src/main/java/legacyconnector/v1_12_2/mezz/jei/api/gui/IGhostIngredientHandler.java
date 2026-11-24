package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 API: drag ingredients out of the list into a mod's GUI (recorded only). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IGhostIngredientHandler<T extends legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen> {
    <I> java.util.List<Target<I>> getTargets(T gui, I ingredient, boolean doStart);
    void onComplete();

    interface Target<I> {
        java.awt.Rectangle getArea();
        void accept(I ingredient);
    }
}
