package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.realms.RealmsScreen;

/**
 * The GuiScreen that showed a Realms screen in 1.12. Realms screens are the modern game's own now, so legacy code only meets this class
 * in {@code instanceof} checks (main menu replacements that hide Realms) and screens it builds itself.
 */
public class GuiScreenRealmsProxy extends GuiScreen {
    private final RealmsScreen proxy;

    public GuiScreenRealmsProxy(RealmsScreen proxyIn) {
        this.proxy = proxyIn;
    }

    public RealmsScreen getProxy() {
        return proxy;
    }
}
