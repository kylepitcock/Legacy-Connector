package legacyconnector.v1_7_10.net.minecraft.realms;

/** The title screen's way into Realms (custom main menus call it from their Realms button): opens the modern Realms screen. */
public class RealmsBridge {
    public RealmsBridge() {
    }

    /** Opens Realms; leaving it returns to the screen showing now (the menu that asked). */
    public void switchToRealms(legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen previous) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.setScreen(new com.mojang.realmsclient.RealmsMainScreen(mc.screen));
    }

    public void init() {
    }
}
