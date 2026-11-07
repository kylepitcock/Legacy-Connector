package legacyconnector.v1_7_10.cpw.mods.fml.client;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;

/** FML's "Mod Options" screen of the pause menu; mods (EnderCore's better mod list) read its parent screen by reflection. */
public class GuiIngameModOptions extends GuiScreen {
    private GuiScreen parentScreen;

    public GuiIngameModOptions(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }
}
