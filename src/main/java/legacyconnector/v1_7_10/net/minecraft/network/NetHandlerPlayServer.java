package legacyconnector.v1_7_10.net.minecraft.network;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;

public class NetHandlerPlayServer implements INetHandler {
    public final EntityPlayerMP playerEntity;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.playerEntity = player;
    }

    public void kickPlayerFromServer(String reason) {
        playerEntity.mcc$serverPlayer().connection.disconnect(new ChatComponentText(reason).mcc$toModern());
    }
}
