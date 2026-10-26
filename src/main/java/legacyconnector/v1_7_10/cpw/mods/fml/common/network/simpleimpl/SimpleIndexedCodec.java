package legacyconnector.v1_7_10.cpw.mods.fml.common.network.simpleimpl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.FMLIndexedMessageToMessageCodec;

/**
 * FML's codec for {@link IMessage}s: the message writes and reads its own bytes. Mods that build their own channel pipeline
 * around it (Fisk's Heroes' network wrapper) sent empty messages while it was only a stub.
 */
public class SimpleIndexedCodec extends FMLIndexedMessageToMessageCodec<IMessage> {
    @Override
    public void encodeInto(ChannelHandlerContext ctx, IMessage msg, ByteBuf target) throws Exception {
        msg.toBytes(target);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, IMessage msg) {
        msg.fromBytes(source);
    }
}
