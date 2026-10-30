package legacyconnector.v1_12_2.net.minecraft.client.gui.inventory;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntitySign;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;

/**
 * 1.12.2's sign editing screen: four lines of up to 90 pixels, arrows and Enter move between lines, Done saves. Displaying this class
 * for a sign of the game shows the modern sign editor; mods' subclasses and legacy signs run this screen, which sends the text to the
 * server as the modern editor does when it closes.
 */
public class GuiEditSign extends GuiScreen {
    private final TileEntitySign tileSign;
    private int updateCounter;
    private int editLine;
    private GuiButton doneBtn;

    public GuiEditSign(TileEntitySign teSign) {
        this.tileSign = teSign;
    }

    public TileEntitySign mcc$sign() {
        return tileSign;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        doneBtn = addButton(new GuiButton(0, width / 2 - 100, height / 4 + 120, I18n.format("gui.done")));
        if (tileSign != null) tileSign.setEditable(false);
    }

    /** Sends the lines to the server (it checks and stores them), as 1.12's CPacketUpdateSign did. */
    @Override
    public void onGuiClosed() {
        if (tileSign == null) return;
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null) {
            String[] lines = new String[4];
            for (int i = 0; i < 4; i++) lines[i] = tileSign.signText[i] == null ? "" : tileSign.signText[i].getUnformattedText();
            var p = tileSign.getPos();
            connection.send(new net.minecraft.network.protocol.game.ServerboundSignUpdatePacket(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()), true,
                    lines[0], lines[1], lines[2], lines[3]));
        }
        tileSign.setEditable(true);
    }

    @Override
    public void updateScreen() {
        ++updateCounter;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && button.id == 0) {
            if (tileSign != null) tileSign.markDirty();
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (tileSign == null) return;
        if (keyCode == 200) editLine = editLine - 1 & 3;
        if (keyCode == 208 || keyCode == 28 || keyCode == 156) editLine = editLine + 1 & 3;
        String s = tileSign.signText[editLine] == null ? "" : tileSign.signText[editLine].getUnformattedText();
        if (keyCode == 14 && !s.isEmpty()) s = s.substring(0, s.length() - 1);
        if (typedChar >= ' ' && typedChar != 127 && typedChar != 167 && fontRenderer.getStringWidth(s + typedChar) <= 90) s = s + typedChar;
        tileSign.signText[editLine] = new TextComponentString(s);
        if (keyCode == 1) actionPerformed(doneBtn);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("sign.edit"), width / 2, 40, 16777215);
        int left = width / 2 - 50;
        int top = 70;
        drawRect(left - 2, top - 2, left + 102, top + 4 * 10 + 2, 0xFF9C7B4A);
        drawRect(left, top, left + 100, top + 4 * 10, 0xFFB8945F);
        if (tileSign != null) {
            for (int i = 0; i < 4; i++) {
                String s = tileSign.signText[i] == null ? "" : tileSign.signText[i].getFormattedText();
                if (i == editLine && updateCounter / 6 % 2 == 0) s = "> " + s + " <";
                fontRenderer.drawString(s, width / 2 - fontRenderer.getStringWidth(s) / 2, top + 1 + i * 10, 0);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
