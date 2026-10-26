package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** 1.7.10's "connection lost" screen: a title, a wrapped message and a button back to the previous screen. */
public class GuiDisconnected extends GuiScreen {
    protected String reason;
    protected IChatComponent message;
    protected List multilineMessage;
    protected final GuiScreen parentScreen;

    public GuiDisconnected(GuiScreen parent, String reasonKey, IChatComponent message) {
        this.parentScreen = parent;
        this.reason = I18n.format(reasonKey);
        this.message = message;
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 120 + 12, I18n.format("gui.toMenu")));
        multilineMessage = fontRendererObj.listFormattedStringToWidth(message == null ? "" : message.getFormattedText(), width - 50);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parentScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, reason, width / 2, height / 2 - 50, 11184810);
        int y = height / 2 - 30;
        if (multilineMessage != null) {
            for (Object line : multilineMessage) {
                drawCenteredString(fontRendererObj, String.valueOf(line), width / 2, y, 16777215);
                y += fontRendererObj.FONT_HEIGHT;
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
