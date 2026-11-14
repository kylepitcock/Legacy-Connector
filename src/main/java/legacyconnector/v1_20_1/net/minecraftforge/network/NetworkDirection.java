package legacyconnector.v1_20_1.net.minecraftforge.network;

import net.neoforged.fml.LogicalSide;

/** Forge 1.20.1's packet directions. */
public enum NetworkDirection {
    PLAY_TO_SERVER(LogicalSide.SERVER),
    PLAY_TO_CLIENT(LogicalSide.CLIENT),
    LOGIN_TO_SERVER(LogicalSide.SERVER),
    LOGIN_TO_CLIENT(LogicalSide.CLIENT);

    private final LogicalSide reception;

    NetworkDirection(LogicalSide reception) {
        this.reception = reception;
    }

    public LogicalSide getReceptionSide() {
        return reception;
    }

    public LogicalSide getOriginationSide() {
        return reception == LogicalSide.CLIENT ? LogicalSide.SERVER : LogicalSide.CLIENT;
    }

    public int getVanillaPacketType() {
        return ordinal() & 1;
    }

    public static NetworkDirection valueOf(int ordinal) {
        return values()[ordinal];
    }

    /**
     * Wraps data a mod encoded itself into the vanilla packet of a channel ({@code PLAY_TO_CLIENT.buildPacket(Pair.of(buffer, index), channelName).getThis()});
     * see {@link RawChannels}.
     */
    @SuppressWarnings("unchecked")
    public <T extends ICustomPacket<?>> T buildPacket(org.apache.commons.lang3.tuple.Pair<net.minecraft.network.FriendlyByteBuf, Integer> packetData,
            net.minecraft.resources.ResourceLocation channelName) {
        return (T) RawChannels.build(this, packetData.getLeft(), packetData.getRight() == null ? 0 : packetData.getRight(), channelName);
    }
}
