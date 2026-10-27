package legacyconnector.v1_7_10.net.minecraft.network;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;

/**
 * A player's server-side connection handler. Mods subclass it for fake players ({@code super(server, fakeManager,
 * player)}: CoFH, EnderIO), overriding {@link #sendPacket} to drop what would go to a client that does not exist.
 */
public class NetHandlerPlayServer implements legacyconnector.v1_7_10.net.minecraft.network.play.INetHandlerPlayServer {
    public final EntityPlayerMP playerEntity;
    public final legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer serverController;
    public final NetworkManager netManager;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.playerEntity = player;
        this.serverController = player == null ? null : player.mcServer;
        // mods read the field directly (Logistics Pipes checks netManager.isChannelOpen() for every watching player)
        this.netManager = mcc$connectionOf(player);
    }

    private static NetworkManager mcc$connectionOf(EntityPlayerMP player) {
        var sp = player == null ? null : player.mcc$serverPlayer();
        return sp == null || sp.connection == null ? NetworkManager.MCC_DUMMY : NetworkManager.mcc$of(sp.connection.getConnection());
    }

    public NetHandlerPlayServer(legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer server, NetworkManager manager, EntityPlayerMP player) {
        this.playerEntity = player;
        this.serverController = server;
        this.netManager = manager;
    }

    /** The player's connection (the given one for handlers mods made, the real one otherwise). */
    public NetworkManager getNetworkManager() {
        if (netManager != null && netManager != NetworkManager.MCC_DUMMY) return netManager;
        return mcc$connectionOf(playerEntity);
    }

    /**
     * Sends a legacy packet to this player: tile entity descriptions become the modern block entity update, packets of a
     * custom channel go through the channel bridge.
     */
    public void sendPacket(Packet packet) {
        var player = playerEntity.mcc$serverPlayer();
        if (player.connection == null) return;
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
        } else if (packet != null) {
            packet.mcc$send(player);
        }
    }

    public void kickPlayerFromServer(String reason) {
        playerEntity.mcc$serverPlayer().connection.disconnect(new ChatComponentText(reason).mcc$toModern());
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        playerEntity.mcc$serverPlayer().connection.teleport(x, y, z, yaw, pitch);
    }

    /**
     * Handles a right-click as if the client had sent it: with the held item in the air (direction 255), or on a block
     * within reach (activating it or using the item on it), then tells the client the blocks' real state.
     */
    public void processPlayerBlockPlacement(legacyconnector.v1_7_10.net.minecraft.network.play.client.C08PacketPlayerBlockPlacement packet) {
        var sp = playerEntity.mcc$serverPlayer();
        var manager = EntityPlayerMP.mcc$get$theItemInWorldManager(playerEntity);
        var world = playerEntity.getServerForPlayer();
        var stack = playerEntity.getCurrentEquippedItem();
        int x = packet.getPlacedBlockX(), y = packet.getPlacedBlockY(), z = packet.getPlacedBlockZ(), side = packet.getPlacedBlockDirection();
        sp.resetLastActionTime();
        if (side == 255) {
            if (stack != null) manager.tryUseItem(playerEntity, world, stack);
        } else {
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            if (y >= sp.serverLevel().getMaxBuildHeight() - 1 && (side == 1 || y >= sp.serverLevel().getMaxBuildHeight())) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable("build.tooHigh", sp.serverLevel().getMaxBuildHeight())
                        .withStyle(net.minecraft.ChatFormatting.RED));
            } else {
                double reach = manager.getBlockReachDistance() + 1.0D;
                if (sp.distanceToSqr(x + 0.5D, y + 0.5D, z + 0.5D) < reach * reach && sp.serverLevel().mayInteract(sp, pos)) {
                    manager.activateBlockOrUseItem(playerEntity, world, stack, x, y, z, side, packet.getPlacedBlockOffsetX(),
                            packet.getPlacedBlockOffsetY(), packet.getPlacedBlockOffsetZ());
                }
            }
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(sp.serverLevel(), pos));
            net.minecraft.core.BlockPos other = pos.relative(net.minecraft.core.Direction.from3DDataValue(side));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(sp.serverLevel(), other));
        }
        sp.containerMenu.broadcastChanges();
    }
}
