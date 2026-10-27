package legacyconnector.v1_12_2.net.minecraft.network;

public enum EnumPacketDirection {
    SERVERBOUND,
    CLIENTBOUND;

    public net.minecraft.network.protocol.PacketFlow mcc$modern() {
        return this == SERVERBOUND ? net.minecraft.network.protocol.PacketFlow.SERVERBOUND : net.minecraft.network.protocol.PacketFlow.CLIENTBOUND;
    }
}
