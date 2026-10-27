package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.FmlChannelBridge1122;

/** The head of a mod channel's pipeline: sends the encoded packets to their target (set through {@link #FML_MESSAGETARGET}). */
public class FMLOutboundHandler extends ChannelOutboundHandlerAdapter {
    public static final AttributeKey<OutboundTarget> FML_MESSAGETARGET = AttributeKey.valueOf("fml:outboundTarget");
    public static final AttributeKey<Object> FML_MESSAGETARGETARGS = AttributeKey.valueOf("fml:outboundTargetArgs");

    /** Where a packet goes; the target args are a player, a dimension id, a target point or an entity. */
    public enum OutboundTarget {
        NOWHERE, PLAYER, ALL, DIMENSION, ALLAROUNDPOINT, TOSERVER, DISPATCHER, REPLY, DISPATCHER_REPLY_TO_ALL, TRACKING_POINT, TRACKING_ENTITY
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        OutboundTarget target = ctx.channel().attr(FML_MESSAGETARGET).get();
        if (!(msg instanceof FMLProxyPacket packet) || target == null || target == OutboundTarget.NOWHERE) {
            ctx.write(msg, promise);
            return;
        }
        Object args = target == OutboundTarget.REPLY ? ctx.channel().attr(NetworkRegistry.NET_HANDLER).get() : ctx.channel().attr(FML_MESSAGETARGETARGS).get();
        FmlChannelBridge1122.send(packet, target, args);
        promise.setSuccess();
    }
}
