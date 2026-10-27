package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.util.IProgressUpdate;

/** 1.12.2's progress screen: a title, the current stage and its percentage, closed when the work is done. */
public class GuiScreenWorking extends GuiScreen implements IProgressUpdate {
    private String title = "";
    private String stage = "";
    private int progress;
    private boolean doneWorking;

    public GuiScreenWorking() {
    }

    @Override
    public void displaySavingString(String displayText) {
        resetProgressAndMessage(displayText);
    }

    @Override
    public void resetProgressAndMessage(String message) {
        title = message == null ? "" : message;
        displayLoadingString("Working...");
    }

    @Override
    public void displayLoadingString(String message) {
        stage = message == null ? "" : message;
        setLoadingProgress(0);
    }

    @Override
    public void setLoadingProgress(int progressIn) {
        progress = progressIn;
    }

    @Override
    public void setDoneWorking() {
        doneWorking = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (doneWorking) {
            if (mc != null) mc.displayGuiScreen(null);
            return;
        }
        drawDefaultBackground();
        drawCenteredString(fontRenderer, title, width / 2, 70, 16777215);
        drawCenteredString(fontRenderer, stage + " " + progress + "%", width / 2, 90, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
