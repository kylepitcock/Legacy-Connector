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

    /** Draws the list (for a screen width) with the objective's scores, through the screen's or HUD's GuiGraphics. */
    public void renderPlayerlist(int width, legacyconnector.v1_12_2.net.minecraft.scoreboard.Scoreboard scoreboardIn,
            legacyconnector.v1_12_2.net.minecraft.scoreboard.ScoreObjective scoreObjectiveIn) {
        var gui = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.gui();
        if (gui == null) return;
        var modernBoard = scoreboardIn != null ? scoreboardIn.mcc$modern() : null;
        if (modernBoard == null) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;
            modernBoard = level.getScoreboard();
        }
        try {
            tab().render(gui, width, modernBoard, scoreObjectiveIn == null ? null : scoreObjectiveIn.mcc$modern());
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing the player list", e);
        }
    }
}
