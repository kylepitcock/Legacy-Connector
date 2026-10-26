package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/** Asks before a link from chat or a book is opened in the browser; a third button copies the link. */
public class GuiConfirmOpenLink extends GuiYesNo {
    private final String openLinkWarning;
    private final String copyLinkButtonText;
    private final String linkText;
    private boolean showSecurityWarning = true;

    public GuiConfirmOpenLink(GuiYesNoCallback callback, String link, int id, boolean fromChat) {
        super(callback, StatCollector.translateToLocal(fromChat ? "chat.link.confirm" : "chat.link.confirmTrusted"), link, id);
        this.confirmButtonText = StatCollector.translateToLocal(fromChat ? "chat.link.open" : "gui.yes");
        this.cancelButtonText = StatCollector.translateToLocal(fromChat ? "gui.cancel" : "gui.no");
        this.copyLinkButtonText = StatCollector.translateToLocal("chat.copy");
        this.openLinkWarning = StatCollector.translateToLocal("chat.link.warning");
        this.linkText = link;
    }

    public String mcc$link() {
        return linkText;
    }

    @Override
    public void initGui() {
        buttonList.add(new GuiButton(0, width / 2 - 50 - 105, height / 6 + 96, 100, 20, confirmButtonText));
        buttonList.add(new GuiButton(2, width / 2 - 50, height / 6 + 96, 100, 20, copyLinkButtonText));
        buttonList.add(new GuiButton(1, width / 2 - 50 + 105, height / 6 + 96, 100, 20, cancelButtonText));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 2) {
            copyLinkToClipboard();
            parentScreen.confirmClicked(false, worldNumber);
            return;
        }
        parentScreen.confirmClicked(button.id == 0, worldNumber);
    }

    public void copyLinkToClipboard() {
        setClipboardString(linkText);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (showSecurityWarning) drawCenteredString(fontRendererObj, openLinkWarning, width / 2, 110, 16764108);
    }

    /** Hides the "never open links from people you don't trust" line (for links the mod itself shows). */
    public void disableSecurityWarning() {
        this.showSecurityWarning = false;
    }
}
