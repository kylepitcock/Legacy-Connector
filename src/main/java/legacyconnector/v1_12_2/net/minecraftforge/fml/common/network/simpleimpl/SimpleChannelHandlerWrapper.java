package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import legacyconnector.v1_12_2.net.minecraft.network.INetHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLOutboundHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.NetworkRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

/**
 * Hands a decoded message of one type to its handler on one side, and sends the reply back over the channel (the pipeline element of
 * FML simple channels, which mods also build their own channels from).
 */
public class SimpleChannelHandlerWrapper<REQ extends IMessage, REPLY extends IMessage> extends SimpleChannelInboundHandler<REQ> {
    private final IMessageHandler<? super REQ, ? extends REPLY> messageHandler;
    private final Side side;

    public SimpleChannelHandlerWrapper(Class<? extends IMessageHandler<? super REQ, ? extends REPLY>> handler, Side side, Class<REQ> requestType) {
        this(mcc$instantiate(handler), side, requestType);
    }

    public SimpleChannelHandlerWrapper(IMessageHandler<? super REQ, ? extends REPLY> handler, Side side, Class<REQ> requestType) {
        super(requestType);
        this.messageHandler = handler;
        this.side = side;
    }

    private static <T> T mcc$instantiate(Class<? extends T> type) {
        try {
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot create the message handler " + type.getName(), e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, REQ msg) throws Exception {
        INetHandler netHandler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
        REPLY result = messageHandler.onMessage(msg, new MessageContext(netHandler, side));
        if (result != null) {
            ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.REPLY);
            ctx.writeAndFlush(result).addListener(io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/FmlChannels").error("A message handler failed on channel {}",
                ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get(), cause);
    }
}
