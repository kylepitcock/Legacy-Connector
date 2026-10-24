package legacyconnector.v1_7_10.net.minecraftforge.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation;

/** A chat message a player sent to the server. */
@Cancelable
public class ServerChatEvent extends Event {
    public final String message;
    public final String username;
    public final EntityPlayerMP player;
    public ChatComponentTranslation component;

    public ServerChatEvent(EntityPlayerMP player, String message, ChatComponentTranslation component) {
        this.message = message;
        this.player = player;
        this.username = player == null ? "" : player.getCommandSenderName();
        this.component = component;
    }
}
