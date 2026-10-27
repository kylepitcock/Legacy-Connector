package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** The player list shown while the list key is held, as a view over the modern tab list: shown state, header and footer. */
public class GuiPlayerTabOverlay extends Gui {
    public static final GuiPlayerTabOverlay mcc$INSTANCE = new GuiPlayerTabOverlay(null, null);

    public GuiPlayerTabOverlay(Minecraft mcIn, GuiIngame guiIngameIn) {
    }

    private static net.minecraft.client.gui.components.PlayerTabOverlay tab() {
        return net.minecraft.client.Minecraft.getInstance().gui.getTabList();
    }

    /** Called with whether the list key is held: the list fades in or out as the modern one does. */
    public void updatePlayerList(boolean willBeVisible) {
        tab().setVisible(willBeVisible);
    }

    public void setFooter(ITextComponent footerIn) {
        tab().setFooter(footerIn == null ? null : footerIn.mcc$modern());
    }

    public void setHeader(ITextComponent headerIn) {
        tab().setHeader(headerIn == null ? null : headerIn.mcc$modern());
    }

    public void resetFooterHeader() {
        tab().reset();
    }
}
