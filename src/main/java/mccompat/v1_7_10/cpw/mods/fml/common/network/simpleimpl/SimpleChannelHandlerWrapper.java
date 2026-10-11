package mccompat.v1_7_10.cpw.mods.fml.common.network.simpleimpl;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mccompat.v1_7_10.cpw.mods.fml.common.network.FMLOutboundHandler;
import mccompat.v1_7_10.cpw.mods.fml.common.network.NetworkRegistry;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.Side;
import mccompat.v1_7_10.net.minecraft.network.NetHandlerPlayServer;

/**
 * FML's inbound handler that hands decoded messages to an {@link IMessageHandler}. Mods that build their own
 * channels with {@code NetworkRegistry.newChannel} add these to the pipeline after their codec.
 */
public class SimpleChannelHandlerWrapper<REQ extends IMessage, REPLY extends IMessage> extends SimpleChannelInboundHandler<REQ> {
    private final IMessageHandler<? super REQ, ? extends REPLY> messageHandler;
    private final Side side;

    public SimpleChannelHandlerWrapper(Class<? extends IMessageHandler<? super REQ, ? extends REPLY>> handler, Side side, Class<REQ> requestType) {
        this(instantiate(handler), side, requestType);
    }

    public SimpleChannelHandlerWrapper(IMessageHandler<? super REQ, ? extends REPLY> handler, Side side, Class<REQ> requestType) {
        super(requestType);
        this.messageHandler = handler;
        this.side = side;
    }

    private static <T> T instantiate(Class<? extends T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate message handler " + type.getName(), e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, REQ msg) {
        Object netHandler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
        NetHandlerPlayServer server = netHandler instanceof NetHandlerPlayServer s ? s : null;
        REPLY result = messageHandler.onMessage(msg, new MessageContext(side, server));
        if (result != null) {
            if (server != null) {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(server.playerEntity);
            } else {
                ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
            }
            ctx.writeAndFlush(result).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }
}
