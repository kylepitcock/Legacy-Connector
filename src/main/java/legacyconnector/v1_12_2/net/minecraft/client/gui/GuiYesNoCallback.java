package legacyconnector.v1_12_2.net.minecraft.client.gui;

/** Answered by a {@code GuiYesNo} or link confirmation: which button and the id the question was asked with. */
public interface GuiYesNoCallback {
    void confirmClicked(boolean result, int id);
}
