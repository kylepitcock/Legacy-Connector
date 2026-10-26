package legacyconnector.v1_7_10.net.minecraft.client.gui.achievement;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiSlot;
import legacyconnector.v1_7_10.net.minecraft.client.gui.IProgressMeter;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.stats.StatFileWriter;

/**
 * 1.7.10's statistics screen. Displaying this class opens the modern statistics screen (see ClientGui), which shows the player's
 * real statistics; a mod's subclass is shown itself, with the title and Done button of the original (its lists were vanilla
 * statistics the modern screen has).
 */
public class GuiStats extends GuiScreen implements IProgressMeter {
    protected static RenderItem renderItem = new RenderItem();
    protected GuiScreen parentScreen;
    protected String screenTitle = "Select world";
    protected GuiSlot displaySlot;
    protected StatFileWriter field_146546_t;
    protected boolean doesGuiPauseGame = true;

    public GuiStats(GuiScreen parent, StatFileWriter writer) {
        this.parentScreen = parent;
        this.field_146546_t = writer;
    }

    public GuiScreen mcc$parent() {
        return parentScreen;
    }

    @Override
    public void initGui() {
        screenTitle = I18n.format("gui.stats");
        doesGuiPauseGame = false;
        createButtons();
    }

    public void createButtons() {
        buttonList.add(new GuiButton(0, width / 2 + 4, height - 28, 150, 20, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == 0) mc.displayGuiScreen(parentScreen);
        else if (displaySlot != null) displaySlot.actionPerformed(button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        if (displaySlot != null) displaySlot.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRendererObj, screenTitle, width / 2, 20, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void doneLoading() {
        doesGuiPauseGame = false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !doesGuiPauseGame;
    }
}
