package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A chat or system message arrived; handlers may replace it or cancel it (it is then not shown). */
@Cancelable
public class ClientChatReceivedEvent extends Event {
    private ITextComponent message;

    public ClientChatReceivedEvent(ITextComponent message) {
        this.message = message;
    }

    public ITextComponent getMessage() {
        return message;
    }

    public void setMessage(ITextComponent message) {
        this.message = message;
    }
}
