package mccompat.v1_7_10.net.minecraft.network;

import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import mccompat.v1_7_10.net.minecraft.util.ChatComponentText;

public class NetHandlerPlayServer {
    public final EntityPlayerMP playerEntity;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.playerEntity = player;
    }

    public void kickPlayerFromServer(String reason) {
        playerEntity.mcc$serverPlayer().connection.disconnect(new ChatComponentText(reason).mcc$toModern());
    }
}
