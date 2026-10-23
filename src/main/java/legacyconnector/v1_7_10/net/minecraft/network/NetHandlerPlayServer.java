package legacyconnector.v1_7_10.net.minecraft.network;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;

public class NetHandlerPlayServer implements INetHandler {
    public final EntityPlayerMP playerEntity;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.playerEntity = player;
    }

    /**
     * Sends a legacy packet to this player: tile entity descriptions become the modern block entity update, packets of a
     * custom channel go through the channel bridge.
     */
    public void sendPacket(Packet packet) {
        var player = playerEntity.mcc$serverPlayer();
        if (packet instanceof legacyconnector.v1_7_10.net.minecraft.network.play.server.S35PacketUpdateTileEntity s35 && s35.getNbtCompound() != null) {
            var pos = new net.minecraft.core.BlockPos(s35.getX(), s35.getY(), s35.getZ());
            var be = player.level().getBlockEntity(pos);
            if (be == null) return;
            net.minecraft.nbt.CompoundTag tag = s35.getNbtCompound().mcc$modern().copy();
            tag.putInt("mcc$type", s35.getTileEntityType());
            player.connection.send(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(be, (b, registries) -> tag));
        } else if (packet instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket fml) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge.send(fml,
                    legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget.PLAYER, playerEntity);
        }
    }

    public void kickPlayerFromServer(String reason) {
        playerEntity.mcc$serverPlayer().connection.disconnect(new ChatComponentText(reason).mcc$toModern());
    }
}
