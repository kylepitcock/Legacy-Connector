package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/**
 * 1.12.2's death screen. Displaying this class itself shows the modern death screen; mods' subclasses run this 1.12 screen: respawn and
 * title screen buttons enabled after a second, the cause of death and the score.
 */
public class GuiGameOver extends GuiScreen {
    private int enableButtonsTimer;
    private final ITextComponent causeOfDeath;

    public GuiGameOver(ITextComponent causeOfDeathIn) {
        this.causeOfDeath = causeOfDeathIn;
    }

    public ITextComponent mcc$cause() {
        return causeOfDeath;
    }

    private static boolean hardcore() {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level != null && level.getLevelData().isHardcore();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        enableButtonsTimer = 0;
        if (hardcore()) {
            buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 72, I18n.format("deathScreen.spectate")));
            buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 96,
                    I18n.format("deathScreen." + (net.minecraft.client.Minecraft.getInstance().hasSingleplayerServer() ? "deleteWorld" : "leaveServer"))));
        } else {
            buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 72, I18n.format("deathScreen.respawn")));
            buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 96, I18n.format("deathScreen.titleScreen")));
        }
        for (GuiButton b : buttonList) b.enabled = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    }

    private static void respawn() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) mc.player.respawn();
        mc.setScreen(null);
    }

    private static void quitToTitle() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) mc.level.disconnect();
        mc.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(net.minecraft.network.chat.Component.translatable("menu.savingLevel")));
        mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            respawn();
        } else if (button.id == 1) {
            if (hardcore()) {
                quitToTitle();
            } else {
                GuiYesNo confirm = new GuiYesNo(this, I18n.format("deathScreen.quit.confirm"), "", I18n.format("deathScreen.titleScreen"),
                        I18n.format("deathScreen.respawn"), 0);
                mc.displayGuiScreen(confirm);
                confirm.setButtonDelay(20);
            }
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) quitToTitle();
        else respawn();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(0, 0, width, height, 1615855616, -1602211792);
        var gui = RenderContext.gui();
        if (gui != null) {
            gui.pose().pushPose();
            gui.pose().scale(2.0F, 2.0F, 2.0F);
            drawCenteredString(fontRenderer, I18n.format(hardcore() ? "deathScreen.title.hardcore" : "deathScreen.title"), width / 2 / 2, 30, 16777215);
            gui.pose().popPose();
        }
        if (causeOfDeath != null) drawCenteredString(fontRenderer, causeOfDeath.getFormattedText(), width / 2, 85, 16777215);
        var player = net.minecraft.client.Minecraft.getInstance().player;
        int score = player == null ? 0 : player.getScore();
        drawCenteredString(fontRenderer, I18n.format("deathScreen.score") + ": §e" + score, width / 2, 100, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ++enableButtonsTimer;
        if (enableButtonsTimer == 20) for (GuiButton b : buttonList) b.enabled = true;
    }
}
