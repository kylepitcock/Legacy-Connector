package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** Receives the answer of a {@link GuiYesNo} screen. */
public interface GuiYesNoCallback {
    void confirmClicked(boolean result, int id);
}
