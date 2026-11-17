package legacyconnector.v1_7_10.net.minecraft.client.gui;

import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** One line of the chat window: its text, the tick it was added and the id a mod gave it (0 for none). */
public class ChatLine {
    private final int updateCounterCreated;
    private final IChatComponent lineString;
    private final int chatLineID;

    public ChatLine(int updateCounter, IChatComponent text, int id) {
        this.lineString = text;
        this.updateCounterCreated = updateCounter;
        this.chatLineID = id;
    }

    public IChatComponent getChatComponent() {
        return lineString;
    }

    public int getUpdatedCounter() {
        return updateCounterCreated;
    }

    public int getChatLineID() {
        return chatLineID;
    }
}
