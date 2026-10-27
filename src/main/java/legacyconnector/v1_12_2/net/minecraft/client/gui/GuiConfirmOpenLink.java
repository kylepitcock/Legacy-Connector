package legacyconnector.v1_12_2.net.minecraft.client.gui;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.client.resources.I18n;

/** Asks before opening a link; "Copy" puts it on the clipboard. The answer goes to the callback, which opens the link itself. */
public class GuiConfirmOpenLink extends GuiYesNo {
    private final String openLinkWarning;
    private final String copyLinkButtonText;
    private final String linkText;
    private boolean showSecurityWarning = true;

    public GuiConfirmOpenLink(GuiYesNoCallback parentScreenIn, String linkTextIn, int parentButtonClickedIdIn, boolean trusted) {
        super(parentScreenIn, I18n.format(trusted ? "chat.link.confirmTrusted" : "chat.link.confirm"), linkTextIn, parentButtonClickedIdIn);
        this.confirmButtonText = I18n.format(trusted ? "chat.link.open" : "gui.yes");
        this.cancelButtonText = I18n.format(trusted ? "gui.cancel" : "gui.no");
        this.copyLinkButtonText = I18n.format("chat.copy");
        this.openLinkWarning = I18n.format("chat.link.warning");
        this.linkText = linkTextIn;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 50 - 105, height / 6 + 96, 100, 20, confirmButtonText));
        buttonList.add(new GuiButton(2, width / 2 - 50, height / 6 + 96, 100, 20, copyLinkButtonText));
        buttonList.add(new GuiButton(1, width / 2 - 50 + 105, height / 6 + 96, 100, 20, cancelButtonText));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 2) copyLinkToClipboard();
        if (parentScreen != null) parentScreen.confirmClicked(button.id == 0, parentButtonClickedId);
    }

    public void copyLinkToClipboard() {
        setClipboardString(linkText);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (showSecurityWarning) drawCenteredString(fontRenderer, openLinkWarning, width / 2, 110, 16764108);
    }

    public void disableSecurityWarning() {
        this.showSecurityWarning = false;
    }
}
