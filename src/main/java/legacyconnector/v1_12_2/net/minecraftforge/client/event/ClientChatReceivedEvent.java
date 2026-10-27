package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A chat or system message arrived; handlers may replace it or cancel it (it is then not shown). */
@Cancelable
public class ClientChatReceivedEvent extends Event {
    private ITextComponent message;

    private final legacyconnector.v1_12_2.net.minecraft.util.text.ChatType type;

    public ClientChatReceivedEvent(ITextComponent message) {
        this(legacyconnector.v1_12_2.net.minecraft.util.text.ChatType.CHAT, message);
    }

    public ClientChatReceivedEvent(legacyconnector.v1_12_2.net.minecraft.util.text.ChatType type, ITextComponent message) {
        this.type = type;
        this.message = message;
    }

    /** Chat from a player, a system message, or action bar text. */
    public legacyconnector.v1_12_2.net.minecraft.util.text.ChatType getType() {
        return type;
    }

    public ITextComponent getMessage() {
        return message;
    }

    public void setMessage(ITextComponent message) {
        this.message = message;
    }
}
