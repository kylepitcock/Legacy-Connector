package mccompat.v1_7_10.net.minecraft.server.management;

import java.util.ArrayList;
import java.util.List;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;
import mccompat.v1_7_10.net.minecraft.util.IChatComponent;
import org.kylepitcock.mccompat.v1_7_10.bridge.EntityBridge;

public class ServerConfigurationManager {
    private final MinecraftServer mcc$server;
    /** Phantom: redirected to {@link #mcc$get$playerEntityList}. */
    @Deprecated
    public List playerEntityList;

    public ServerConfigurationManager(MinecraftServer server) {
        this.mcc$server = server;
    }

    public static List mcc$get$playerEntityList(ServerConfigurationManager m) {
        List<EntityPlayerMP> out = new ArrayList<>();
        for (var p : m.mcc$server.mcc$modern().getPlayerList().getPlayers()) out.add((EntityPlayerMP) EntityBridge.wrap(p));
        return out;
    }

    public EntityPlayerMP getPlayerByUsername(String name) {
        var p = mcc$server.mcc$modern().getPlayerList().getPlayerByName(name);
        return p == null ? null : (EntityPlayerMP) EntityBridge.wrap(p);
    }

    public void sendChatMsg(IChatComponent message) {
        mcc$server.mcc$modern().getPlayerList().broadcastSystemMessage(message.mcc$toModern(), false);
    }

    public void sendChatMsgImpl(IChatComponent message, boolean system) {
        sendChatMsg(message);
    }

    public int getCurrentPlayerCount() {
        return mcc$server.getCurrentPlayerCount();
    }

    public String[] getAllUsernames() {
        return mcc$server.getAllUsernames();
    }

    public boolean canSendCommands(com.mojang.authlib.GameProfile profile) {
        return mcc$server.mcc$modern().getPlayerList().isOp(profile);
    }
}
