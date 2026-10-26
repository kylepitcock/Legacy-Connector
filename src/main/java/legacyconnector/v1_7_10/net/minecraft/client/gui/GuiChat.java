package legacyconnector.v1_7_10.net.minecraft.client.gui;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;

/**
 * 1.7.10's chat input line. Displaying a plain GuiChat opens the modern chat screen (see ClientGui); mods' subclasses (a
 * map's chat overlay) run this implementation: the input field at the bottom, sent-message history, Enter sends the line
 * (client commands first).
 */
public class GuiChat extends GuiScreen implements GuiYesNoCallback {
    protected String defaultInputFieldText = "";
    protected GuiTextField inputField;
    private String historyBuffer = "";
    private int sentHistoryCursor = -1;

    public GuiChat() {
    }

    public GuiChat(String defaultText) {
        this.defaultInputFieldText = defaultText;
    }

    public String mcc$defaultText() {
        return defaultInputFieldText;
    }

    @Override
    public void initGui() {
        sentHistoryCursor = mc.ingameGUI.getChatGUI().getSentMessages().size();
        inputField = new GuiTextField(fontRendererObj, 4, height - 12, width - 4, 12);
        inputField.setMaxStringLength(256);
        inputField.setEnableBackgroundDrawing(false);
        inputField.setFocused(true);
        inputField.setText(defaultInputFieldText);
        inputField.setCanLoseFocus(false);
    }

    @Override
    public void updateScreen() {
        if (inputField != null) inputField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
        } else if (keyCode == 28 || keyCode == 156) {
            String line = inputField.getText().trim();
            if (!line.isEmpty()) func_146403_a(line);
            mc.displayGuiScreen(null);
        } else if (keyCode == 200) {
            getSentHistory(-1);
        } else if (keyCode == 208) {
            getSentHistory(1);
        } else {
            inputField.textboxKeyTyped(c, keyCode);
        }
    }

    /** Sends a line typed into the chat: a client command if one matches, else to the server. */
    public void func_146403_a(String line) {
        mc.ingameGUI.getChatGUI().addToSentMessages(line);
        var player = Minecraft.mcc$get$thePlayer(mc);
        if (player == null) return;
        if (legacyconnector.v1_7_10.net.minecraftforge.client.ClientCommandHandler.instance.executeCommand(player, line) != 0) return;
        player.sendChatMessage(line);
    }

    /** Moves through the sent messages (-1 older, 1 newer), keeping what was being typed. */
    public void getSentHistory(int step) {
        List sent = mc.ingameGUI.getChatGUI().getSentMessages();
        int target = Math.max(0, Math.min(sent.size(), sentHistoryCursor + step));
        if (target == sentHistoryCursor) return;
        if (target == sent.size()) {
            sentHistoryCursor = target;
            inputField.setText(historyBuffer);
        } else {
            if (sentHistoryCursor == sent.size()) historyBuffer = inputField.getText();
            inputField.setText(String.valueOf(sent.get(target)));
            sentHistoryCursor = target;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (inputField != null) inputField.mouseClicked(mouseX, mouseY, button);
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(2, height - 14, width - 2, height - 2, Integer.MIN_VALUE);
        if (inputField != null) inputField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
