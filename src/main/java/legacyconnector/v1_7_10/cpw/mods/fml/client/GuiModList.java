package legacyconnector.v1_7_10.cpw.mods.fml.client;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;

/** FML's mod list; displaying it opens NeoForge's mod list (see ClientGui). Mods subclass it for their own mod lists. */
public class GuiModList extends GuiScreen {
    protected GuiScreen mainMenu;

    public GuiModList(GuiScreen mainMenu) {
        this.mainMenu = mainMenu;
    }

    public GuiScreen mcc$parent() {
        return mainMenu;
    }
}
