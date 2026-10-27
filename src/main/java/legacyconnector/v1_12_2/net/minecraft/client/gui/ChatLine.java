package legacyconnector.v1_12_2.net.minecraft.client.gui;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** One drawn line of the chat: its text, the HUD tick it was added at and the id it was printed with (0 for none). */
public class ChatLine {
    private final int updateCounterCreated;
    private final ITextComponent lineString;
    private final int chatLineID;

    public ChatLine(int updateCounterCreatedIn, ITextComponent lineStringIn, int chatLineIDIn) {
        this.lineString = lineStringIn;
        this.updateCounterCreated = updateCounterCreatedIn;
        this.chatLineID = chatLineIDIn;
    }

    public ITextComponent getChatComponent() {
        return lineString;
    }

    public int getUpdatedCounter() {
        return updateCounterCreated;
    }

    public int getChatLineID() {
        return chatLineID;
    }
}
