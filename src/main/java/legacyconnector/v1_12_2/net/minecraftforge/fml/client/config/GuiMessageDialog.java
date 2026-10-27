package legacyconnector.v1_12_2.net.minecraftforge.fml.client.config;

import java.io.IOException;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A message with one button that continues to the next screen (the config screens' "restart required" notice). */
public class GuiMessageDialog extends GuiScreen {
    protected final GuiScreen nextScreen;
    protected final String title;
    protected final ITextComponent message;
    protected String buttonText;
    private List<String> lines;

    public GuiMessageDialog(GuiScreen nextScreen, String title, ITextComponent message, String buttonText) {
        this.nextScreen = nextScreen;
        this.title = I18n.format(title);
        this.message = message;
        this.buttonText = buttonText;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        lines = fontRenderer.listFormattedStringToWidth(message == null ? "" : message.getFormattedText(), width - 50);
        int textHeight = lines.size() * fontRenderer.FONT_HEIGHT;
        buttonList.add(new GuiButton(0, width / 2 - 100, Math.min(height / 2 + textHeight / 2 + fontRenderer.FONT_HEIGHT, height - 30), I18n.format(buttonText)));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) mc.displayGuiScreen(nextScreen);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int textHeight = lines.size() * fontRenderer.FONT_HEIGHT;
        drawCenteredString(fontRenderer, title, width / 2, height / 2 - textHeight / 2 - fontRenderer.FONT_HEIGHT * 2, 11184810);
        int y = height / 2 - textHeight / 2;
        for (String s : lines) {
            drawCenteredString(fontRenderer, s, width / 2, y, 16777215);
            y += fontRenderer.FONT_HEIGHT;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
