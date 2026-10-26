package legacyconnector.v1_7_10.cpw.mods.fml.client.config;

import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiDisconnected;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_7_10.net.minecraft.client.resources.I18n;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** A message with one button that continues to the next screen (the config screens' "restart required" notice). */
public class GuiMessageDialog extends GuiDisconnected {
    protected String buttonText;

    public GuiMessageDialog(GuiScreen nextScreen, String title, IChatComponent message, String buttonText) {
        super(nextScreen, title, message);
        this.buttonText = buttonText;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!buttonList.isEmpty()) ((GuiButton) buttonList.get(0)).displayString = I18n.format(buttonText);
    }
}
