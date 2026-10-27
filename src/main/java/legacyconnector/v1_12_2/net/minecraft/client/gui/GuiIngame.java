package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;

/** The in-game HUD ({@code Minecraft.ingameGUI}) as a view over the modern one: chat, action bar and titles. */
public class GuiIngame extends Gui {
    public static final GuiIngame mcc$INSTANCE = new GuiIngame(null);

    public GuiIngame(Minecraft mcIn) {
    }

    private static net.minecraft.client.gui.Gui hud() {
        return net.minecraft.client.Minecraft.getInstance().gui;
    }

    public GuiNewChat getChatGUI() {
        return GuiNewChat.mcc$INSTANCE;
    }

    public int getUpdateCounter() {
        return hud().getGuiTicks();
    }

    public FontRenderer getFontRenderer() {
        return FontRenderer.mcc$INSTANCE;
    }

    /** The action bar text ({@code animateColor}: the jukebox's rainbow). */
    public void setOverlayMessage(String message, boolean animateColor) {
        hud().setOverlayMessage(net.minecraft.network.chat.Component.literal(message), animateColor);
    }

    public void setOverlayMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component, boolean animateColor) {
        if (component != null) hud().setOverlayMessage(component.mcc$modern(), animateColor);
    }

    public void setRecordPlayingMessage(String recordName) {
        hud().setNowPlaying(net.minecraft.network.chat.Component.literal(recordName));
    }

    public void displayTitle(String title, String subTitle, int timeFadeIn, int displayTime, int timeFadeOut) {
        var h = hud();
        if (title == null && subTitle == null && timeFadeIn < 0 && displayTime < 0 && timeFadeOut < 0) {
            h.clear();
            return;
        }
        if (title != null) h.setTitle(net.minecraft.network.chat.Component.literal(title));
        if (subTitle != null) h.setSubtitle(net.minecraft.network.chat.Component.literal(subTitle));
        if (timeFadeIn >= 0 || displayTime >= 0 || timeFadeOut >= 0) h.setTimes(timeFadeIn, displayTime, timeFadeOut);
    }
}
