package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

/** One discriminator byte, then the message's own bytes (FML's wire format). */
@ChannelHandler.Sharable
public abstract class FMLIndexedMessageToMessageCodec<A> extends MessageToMessageCodec<FMLProxyPacket, A> {
    public static final io.netty.util.AttributeKey<ThreadLocal<java.lang.ref.WeakReference<FMLProxyPacket>>> INBOUNDPACKETTRACKER =
            io.netty.util.AttributeKey.valueOf("fml:inboundpacket");

    private final Byte2ObjectMap<Class<? extends A>> discriminators = new Byte2ObjectOpenHashMap<>();
    private final Object2ByteMap<Class<? extends A>> types = new Object2ByteOpenHashMap<>();

    public FMLIndexedMessageToMessageCodec() {
    }

    public FMLIndexedMessageToMessageCodec<A> addDiscriminator(int discriminator, Class<? extends A> type) {
        discriminators.put((byte) discriminator, type);
        types.put(type, (byte) discriminator);
        return this;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        ctx.channel().attr(INBOUNDPACKETTRACKER).set(new ThreadLocal<>());
    }

    public abstract void encodeInto(ChannelHandlerContext ctx, A msg, ByteBuf target) throws Exception;

    public abstract void decodeInto(ChannelHandlerContext ctx, ByteBuf source, A msg);

    @Override
    @SuppressWarnings("unchecked")
    protected final void encode(ChannelHandlerContext ctx, A msg, List<Object> out) throws Exception {
        String channel = ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get();
        Class<? extends A> clazz = (Class<? extends A>) msg.getClass();
        if (!types.containsKey(clazz)) throw new RuntimeException("Undefined discriminator for message type " + clazz.getSimpleName() + " in channel " + channel);
        ByteBuf buffer = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.buffer();
        buffer.writeByte(types.getByte(clazz));
        encodeInto(ctx, msg, buffer);
        FMLProxyPacket proxy = new FMLProxyPacket(buffer.copy(), channel);
        var inbound = ctx.channel().attr(INBOUNDPACKETTRACKER).get();
        if (inbound != null && inbound.get() != null && inbound.get().get() != null) proxy.mcc$setHandler(inbound.get().get().handler());
        out.add(proxy);
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
        var tracker = ctx.channel().attr(INBOUNDPACKETTRACKER).get();
        if (tracker != null) tracker.set(new java.lang.ref.WeakReference<>(msg));
        ByteBuf payload = msg.payload().duplicate();
        if (payload.readableBytes() < 1) return;
        byte discriminator = payload.readByte();
        Class<? extends A> clazz = discriminators.get(discriminator);
        if (clazz == null) throw new NullPointerException("Undefined message for discriminator " + discriminator + " in channel " + msg.channel());
        var ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        A newMsg = ctor.newInstance();
        decodeInto(ctx, payload.slice(), newMsg);
        out.add(newMsg);
    }
}
