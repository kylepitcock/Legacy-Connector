package legacyconnector.v1_12_2.net.minecraft.realms;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreenRealmsProxy;

/** A Realms screen of 1.12 (shown through a {@link GuiScreenRealmsProxy}); the Realms client itself is the modern game's. */
public class RealmsScreen {
    protected GuiScreenRealmsProxy proxy = new GuiScreenRealmsProxy(this);

    public GuiScreenRealmsProxy getProxy() {
        return proxy;
    }
}
