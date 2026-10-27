package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;

/** A red error message with a button back to the main menu. */
public class GuiErrorScreen extends GuiScreen {
    private final String title;
    private final String message;

    public GuiErrorScreen(String titleIn, String messageIn) {
        this.title = titleIn;
        this.message = messageIn;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, width / 2 - 100, 140, I18n.format("gui.cancel")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(0, 0, width, height, -12574688, -11530224);
        drawCenteredString(fontRenderer, title, width / 2, 90, 16777215);
        drawCenteredString(fontRenderer, message, width / 2, 110, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        mc.displayGuiScreen(null);
        net.minecraft.client.Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }
}
