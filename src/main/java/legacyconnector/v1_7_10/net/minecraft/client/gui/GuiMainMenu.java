package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;

/**
 * 1.7.10 title screen. The real title screen is the modern one, and mods that replace it (Custom Main Menu) see this in the
 * GuiOpenEvent and swap it for a screen of their own. Mods whose menu EXTENDS it (The Betweenlands' {@code GuiBLMainMenu}) call
 * {@code super.initGui()} / {@code super.drawScreen()} and expect vanilla's buttons, so it has them: singleplayer, multiplayer,
 * options, quit and language, each opening the shim screen that shows the modern one (see ClientGui).
 */
public class GuiMainMenu extends GuiScreen {
    @Override
    public void initGui() {
        int y = this.height / 4 + 48;
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, y, I18n.format("menu.singleplayer")));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 100, y + 24, I18n.format("menu.multiplayer")));
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, y + 72 + 12, 98, 20, I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(4, this.width / 2 + 2, y + 72 + 12, 98, 20, I18n.format("menu.quit")));
        this.buttonList.add(new GuiButton(5, this.width / 2 - 124, y + 72 + 12, 20, 20, ""));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Minecraft mc = this.mc != null ? this.mc : Minecraft.getMinecraft();
        switch (button.id) {
            case 0 -> mc.displayGuiScreen(new GuiOptions(this, Minecraft.mcc$get$gameSettings(mc)));
            case 1 -> mc.displayGuiScreen(new GuiSelectWorld(this));
            case 2 -> mc.displayGuiScreen(new GuiMultiplayer(this));
            case 4 -> mc.shutdown();
            case 5 -> mc.displayGuiScreen(new GuiLanguage(this, Minecraft.mcc$get$gameSettings(mc), mc.getLanguageManager()));
            default -> { }
        }
    }
}
