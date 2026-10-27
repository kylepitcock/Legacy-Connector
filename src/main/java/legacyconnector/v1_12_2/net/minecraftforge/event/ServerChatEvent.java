package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player's chat message on the server: canceling drops it, a new component replaces what everyone sees. */
@Cancelable
public class ServerChatEvent extends Event {
    private final String message;
    private final String username;
    private final EntityPlayerMP player;
    private ITextComponent component;

    public ServerChatEvent(EntityPlayerMP player, String message, ITextComponent component) {
        this.message = message;
        this.player = player;
        this.username = player.getName();
        this.component = component;
    }

    public void setComponent(ITextComponent e) {
        this.component = e;
    }

    public ITextComponent getComponent() {
        return component;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public EntityPlayerMP getPlayer() {
        return player;
    }
}
