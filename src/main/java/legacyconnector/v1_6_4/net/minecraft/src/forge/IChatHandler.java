package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IChatHandler {
    String onServerChat(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, String message);

    boolean onChatCommand(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player, boolean isOp, String command);

    boolean onServerCommand(Object listener, String username, String command);

    String onServerCommandSay(Object listener, String username, String message);

    String onClientChatRecv(String message);
}
