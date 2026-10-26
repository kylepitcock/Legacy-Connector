package legacyconnector.v1_7_10.net.minecraft.client.gui;

/** A two-line error message with a cancel button that goes back to the title screen. */
public class GuiErrorScreen extends GuiScreen {
    private String field_146313_a;
    private String field_146312_f;

    public GuiErrorScreen(String title, String message) {
        this.field_146313_a = title;
        this.field_146312_f = message;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiButton(0, width / 2 - 100, 140, legacyconnector.v1_7_10.net.minecraft.client.resources.I18n.format("gui.cancel")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(0, 0, width, height, -12574688, -11530224);
        drawCenteredString(fontRendererObj, field_146313_a, width / 2, 90, 16777215);
        drawCenteredString(fontRendererObj, field_146312_f, width / 2, 110, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }
}
