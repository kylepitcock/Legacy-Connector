package legacyconnector.v1_6_4.facade.net.minecraft.client.gui;

/** Facade of the 1.7.10 shim's {@code ChatLine} for 1.6.4 and older mods: the line was a plain string there, 1.7.10 holds a chat component. Never instantiated. */
public final class ChatLine {
    private ChatLine() {}

    /** {@code getChatLineString()}: the text of the line with its formatting codes. */
    public static String getChatLineString(legacyconnector.v1_7_10.net.minecraft.client.gui.ChatLine self) {
        var text = self.getChatComponent();
        return text == null ? "" : text.getFormattedText();
    }

    /** The 1.2.5 field {@code lineString} (the text of the line). */
    public static String mcc$get$lineString(legacyconnector.v1_7_10.net.minecraft.client.gui.ChatLine self) {
        return getChatLineString(self);
    }
}
