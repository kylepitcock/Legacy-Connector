package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** The end poem and credits. Displaying it shows the modern credits screen, which runs {@code onFinished} when it closes. */
public class GuiWinGame extends GuiScreen {
    private final boolean poem;
    private final Runnable onFinished;

    public GuiWinGame(boolean poemIn, Runnable onFinishedIn) {
        this.poem = poemIn;
        this.onFinished = onFinishedIn;
    }

    /** The modern credits screen this one stands for. */
    public net.minecraft.client.gui.screens.Screen mcc$modern() {
        return new net.minecraft.client.gui.screens.WinScreen(poem, onFinished == null ? () -> net.minecraft.client.Minecraft.getInstance().setScreen(null) : onFinished);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
