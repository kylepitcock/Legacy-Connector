package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.IProgressUpdate;

/** 1.7.10's progress screen (world conversion, terrain download): a title and a working message centred on the dirt background. */
public class GuiScreenWorking extends GuiScreen implements IProgressUpdate {
    private String field_146591_a = "";
    private String field_146589_f = "";
    private int field_146590_g;
    private boolean field_146592_h;

    public GuiScreenWorking() {
    }

    @Override
    public void displayProgressMessage(String message) {
        resetProgressAndMessage(message);
    }

    @Override
    public void resetProgressAndMessage(String message) {
        this.field_146591_a = message == null ? "" : message;
        resetProgresAndWorkingMessage("Working...");
    }

    @Override
    public void resetProgresAndWorkingMessage(String message) {
        this.field_146589_f = message == null ? "" : message;
        setLoadingProgress(0);
    }

    @Override
    public void setLoadingProgress(int percent) {
        this.field_146590_g = percent;
    }

    @Override
    public void setDoneWorking() {
        this.field_146592_h = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.field_146592_h) {
            mc.displayGuiScreen(null);
            return;
        }
        drawBackground(0);
        drawCenteredString(fontRendererObj, this.field_146591_a, width / 2, 70, 16777215);
        drawCenteredString(fontRendererObj, this.field_146589_f + " " + this.field_146590_g + "%", width / 2, 90, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
