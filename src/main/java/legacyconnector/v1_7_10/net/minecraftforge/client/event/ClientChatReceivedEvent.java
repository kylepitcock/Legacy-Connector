package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/** Fired when a chat message reaches the client; handlers may replace the message, or cancel it to hide it. */
@Cancelable
public class ClientChatReceivedEvent extends Event {
    public IChatComponent message;

    public ClientChatReceivedEvent(IChatComponent message) {
        this.message = message;
    }
}
