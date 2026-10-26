package legacyconnector.v1_7_10.cpw.mods.fml.client;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiYesNo;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiYesNoCallback;

/**
 * FML's "this world has pre-update mod data, load it anyway?" question. Continue loads the world (the modern game makes
 * its own backup prompt for worlds that need one); cancel goes back to the parent screen.
 */
public class GuiOldSaveLoadConfirm extends GuiYesNo implements GuiYesNoCallback {
    private final String dirName;
    private final String saveName;
    private final GuiScreen parent;

    public GuiOldSaveLoadConfirm(String dirName, String saveName, GuiScreen parent) {
        super(null, "", "", 0);
        this.parent = parent;
        this.dirName = dirName;
        this.saveName = saveName;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, String.format("The world %s contains pre-update modding data", saveName), width / 2, 50, 16777215);
        drawCenteredString(fontRendererObj, "There may be problems updating it to this version", width / 2, 70, 16777215);
        drawCenteredString(fontRendererObj, "Do you wish to continue loading?", width / 2, 110, 16777215);
        for (Object button : buttonList) ((GuiButton) button).drawButton(mc, mouseX, mouseY);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            FMLClientHandler.instance().showGuiScreen(parent);
        } else {
            FMLClientHandler.instance().showGuiScreen(null);
            mc.launchIntegratedServer(dirName, saveName, null);
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
    }
}
