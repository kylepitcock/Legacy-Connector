package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** The player is sending a chat message or command; handlers may change it or cancel sending. */
@Cancelable
public class ClientChatEvent extends Event {
    private String message;
    private final String originalMessage;

    public ClientChatEvent(String message) {
        this.message = message;
        this.originalMessage = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }
}
