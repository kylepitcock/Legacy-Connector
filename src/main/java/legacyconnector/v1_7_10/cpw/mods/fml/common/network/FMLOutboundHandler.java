package legacyconnector.v1_7_10.cpw.mods.fml.common.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.internal.FMLProxyPacket;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FmlChannelBridge;

/** Sends encoded packets to their target (set through {@link #FML_MESSAGETARGET}) over the modern network. */
public class FMLOutboundHandler extends ChannelOutboundHandlerAdapter {
    public static final AttributeKey<OutboundTarget> FML_MESSAGETARGET = AttributeKey.valueOf("fml:outboundTarget");
    public static final AttributeKey<Object> FML_MESSAGETARGETARGS = AttributeKey.valueOf("fml:outboundTargetArgs");

    /** Where a packet goes: DISPATCHER to the dispatcher in the target args, REPLY to the sender of the message being handled. */
    public enum OutboundTarget {
        NOWHERE, PLAYER, ALL, DIMENSION, ALLAROUNDPOINT, TOSERVER, DISPATCHER, REPLY
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        OutboundTarget target = ctx.channel().attr(FML_MESSAGETARGET).get();
        if (!(msg instanceof FMLProxyPacket packet) || target == null || target == OutboundTarget.NOWHERE) {
            ctx.write(msg, promise);
            return;
        }
        Object args = target == OutboundTarget.REPLY
                ? ctx.channel().attr(legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher.FML_DISPATCHER).get()
                : ctx.channel().attr(FML_MESSAGETARGETARGS).get();
        if (packet.getDispatcher() == null && args instanceof legacyconnector.v1_7_10.cpw.mods.fml.common.network.handshake.NetworkDispatcher d) {
            packet.setDispatcher(d);
        }
        FmlChannelBridge.send(packet, target, args);
        promise.setSuccess();
    }
}
