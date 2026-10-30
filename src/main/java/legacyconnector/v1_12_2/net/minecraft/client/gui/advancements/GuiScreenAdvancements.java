package legacyconnector.v1_12_2.net.minecraft.client.gui.advancements;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.advancements.Advancement;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ClientAdvancementManager;

/**
 * 1.12.2's advancements screen. Displaying it shows the modern advancements screen (its tabs, trees and progress); the class keeps the
 * manager and tab selection for code that drives it, and closes on Escape or the advancements key like 1.12's.
 */
public class GuiScreenAdvancements extends GuiScreen {
    private final ClientAdvancementManager clientAdvancementManager;

    public GuiScreenAdvancements(ClientAdvancementManager manager) {
        this.clientAdvancementManager = manager;
    }

    /** Opens the advancement's tab (the server is told, as 1.12's screen did). */
    public void setSelectedTab(Advancement advancement) {
        if (clientAdvancementManager != null) clientAdvancementManager.setSelectedTab(advancement, true);
    }

    @Override
    public void onGuiClosed() {
        if (clientAdvancementManager != null) clientAdvancementManager.setListener(null);
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null) connection.send(net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket.closedScreen());
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        var mcm = net.minecraft.client.Minecraft.getInstance();
        int glfw = keyCode > 0 ? org.kylepitcock.legacyconnector.v1_7_10.client.KeyCodes.toGlfw(keyCode) : -1;
        if (keyCode == 1 || glfw > 0 && mcm.options.keyAdvancements.matches(glfw, 0)) {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
}
