package legacyconnector.v1_12_2.net.minecraft.network;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

public class NetHandlerPlayServer {
    public EntityPlayerMP player;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.player = player;
    }
}
