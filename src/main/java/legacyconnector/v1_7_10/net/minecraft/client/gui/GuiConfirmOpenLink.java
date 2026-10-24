package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.StatCollector;

/** Asks before a link from chat or a book is opened in the browser. */
public class GuiConfirmOpenLink extends GuiYesNo {
    private final String linkText;

    public GuiConfirmOpenLink(GuiYesNoCallback callback, String link, int id, boolean fromChat) {
        super(callback, StatCollector.translateToLocal(fromChat ? "chat.link.confirm" : "chat.link.confirmTrusted"), link, id);
        this.confirmButtonText = StatCollector.translateToLocal(fromChat ? "chat.link.open" : "gui.yes");
        this.cancelButtonText = StatCollector.translateToLocal(fromChat ? "gui.cancel" : "gui.no");
        this.linkText = link;
    }

    public String mcc$link() {
        return linkText;
    }
}
