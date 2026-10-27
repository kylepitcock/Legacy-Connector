package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;

/** FML's mod list; displaying it opens NeoForge's mod list with the open screen as its parent. Mods subclass it for their own mod lists. */
public class GuiModList extends GuiScreen {
    protected GuiScreen mainMenu;

    public GuiModList(GuiScreen mainMenu) {
        this.mainMenu = mainMenu;
    }

    public GuiScreen mcc$parent() {
        return mainMenu;
    }
}
