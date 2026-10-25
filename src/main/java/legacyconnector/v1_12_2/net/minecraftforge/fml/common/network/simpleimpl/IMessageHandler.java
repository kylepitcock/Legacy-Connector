package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network.simpleimpl;

public interface IMessageHandler<REQ extends IMessage, REPLY extends IMessage> {
    REPLY onMessage(REQ message, MessageContext ctx);
}
