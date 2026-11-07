package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import legacyconnector.v1_6_4.net.minecraft.util.ChatMessageComponent;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/**
 * Targets of the loader's 1.6.4 call redirects ({@code legacyconnector/forward/1.6.4/call-redirects.tsv}): static methods taking the
 * receiver of the redirected call as an {@code Object}.
 */
public final class Redirects164 {
    private Redirects164() {}

    /** {@code sender.sendChatToPlayer(ChatMessageComponent)}: 1.7.10 sends {@code IChatComponent}s. */
    public static void sendChatToPlayer(Object receiver, ChatMessageComponent message) {
        if (message == null) return;
        IChatComponent component = message.mcc$toLegacy();
        if (receiver instanceof ICommandSender sender) sender.addChatMessage(component);
        else if (receiver instanceof EntityPlayer player) player.addChatComponentMessage(component);
    }

    /** {@code world.getWorldVec3Pool()}: the shared stateless pool. */
    public static legacyconnector.v1_6_4.net.minecraft.util.Vec3Pool getWorldVec3Pool(Object world) {
        return legacyconnector.v1_6_4.net.minecraft.util.Vec3Pool.INSTANCE;
    }

    /** {@code AxisAlignedBB.getAABBPool()}: the shared stateless pool. */
    public static legacyconnector.v1_6_4.net.minecraft.util.AABBPool getAABBPool() {
        return legacyconnector.v1_6_4.net.minecraft.util.AABBPool.INSTANCE;
    }

    /** Forge 1.5.2's {@code MinecraftForge.setBlockHarvestLevel(block, toolClass, level)}. */
    public static void setBlockHarvestLevel(legacyconnector.v1_7_10.net.minecraft.block.Block block, String toolClass, int level) {
        if (block != null) block.setHarvestLevel(toolClass, level);
    }

    /** ... and the variant for one metadata value. */
    public static void setBlockHarvestLevel(legacyconnector.v1_7_10.net.minecraft.block.Block block, int metadata, String toolClass, int level) {
        if (block != null) block.setHarvestLevel(toolClass, level, metadata);
    }




    /** {@code netServerHandler.sendPacketToPlayer(Packet)} is 1.7.10's {@code sendPacket}. */
    public static void sendPacketToPlayer(Object handler, Packet packet) {
        if (handler instanceof NetHandlerPlayServer server && packet != null) server.sendPacket(packet);
    }
}
