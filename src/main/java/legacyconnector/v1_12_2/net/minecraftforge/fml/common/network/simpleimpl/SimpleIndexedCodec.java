package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec;

/** The codec of a simple channel: a message writes and reads its own bytes after the discriminator. */
public class SimpleIndexedCodec extends FMLIndexedMessageToMessageCodec<IMessage> {
    public SimpleIndexedCodec() {
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, IMessage msg, ByteBuf target) throws Exception {
        msg.toBytes(target);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, IMessage msg) {
        msg.fromBytes(source);
    }
}
