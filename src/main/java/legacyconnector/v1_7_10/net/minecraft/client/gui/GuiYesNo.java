package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/** A two-line question with a confirm and a cancel button. */
public class GuiYesNo extends GuiScreen {
    protected GuiYesNoCallback parentScreen;
    protected String messageLine1;
    private final String messageLine2;
    protected String confirmButtonText;
    protected String cancelButtonText;
    protected int worldNumber;

    public GuiYesNo(GuiYesNoCallback callback, String line1, String line2, int id) {
        this(callback, line1, line2, StatCollector.translateToLocal("gui.yes"), StatCollector.translateToLocal("gui.no"), id);
    }

    public GuiYesNo(GuiYesNoCallback callback, String line1, String line2, String confirm, String cancel, int id) {
        this.parentScreen = callback;
        this.messageLine1 = line1;
        this.messageLine2 = line2;
        this.confirmButtonText = confirm;
        this.cancelButtonText = cancel;
        this.worldNumber = id;
    }

    @Override
    public void initGui() {
        buttonList.add(new GuiButton(0, width / 2 - 155, height / 6 + 96, 150, 20, confirmButtonText));
        buttonList.add(new GuiButton(1, width / 2 + 5, height / 6 + 96, 150, 20, cancelButtonText));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        parentScreen.confirmClicked(button.id == 0, worldNumber);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, messageLine1, width / 2, 70, 0xFFFFFF);
        drawCenteredString(fontRendererObj, messageLine2, width / 2, 90, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
