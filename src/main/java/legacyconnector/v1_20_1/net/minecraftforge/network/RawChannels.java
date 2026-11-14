package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The channels a mod can address by name ({@code NetworkDirection.buildPacket(Pair.of(buffer, index), channelName)}): every {@code SimpleChannel} and
 * {@code EventNetworkChannel} registers a factory that wraps raw bytes into its own payload type, because NeoForge 1.21.1 can only encode packets of a registered
 * payload type (Forge 1.20.1 sent any byte buffer under any channel name).
 */
public final class RawChannels {
    /** Builds the payload of a channel from the data a mod encoded itself; {@code index} is the message index of a {@code SimpleChannel} (0 for event channels). */
    @FunctionalInterface
    public interface Factory {
        CustomPacketPayload payload(FriendlyByteBuf data, int index);
    }

    private static final Map<ResourceLocation, Factory> FACTORIES = new ConcurrentHashMap<>();

    private RawChannels() {}

    public static void register(ResourceLocation name, Factory factory) {
        FACTORIES.put(name, factory);
    }

    /** The vanilla packet carrying a payload in the given direction. */
    public static Packet<?> wrap(NetworkDirection direction, CustomPacketPayload payload) {
        return direction.getReceptionSide() == net.neoforged.fml.LogicalSide.CLIENT ? new ClientboundCustomPayloadPacket(payload) : new ServerboundCustomPayloadPacket(payload);
    }

    /** {@code NetworkDirection.buildPacket}: the packet of a channel this layer knows; a channel nobody registered gets a packet that is {@code null} (it cannot be encoded). */
    public static ICustomPacket<Packet<?>> build(NetworkDirection direction, FriendlyByteBuf data, int index, ResourceLocation channel) {
        Factory f = FACTORIES.get(channel);
        Packet<?> packet = f == null ? null : wrap(direction, f.payload(data, index));
        return new RawPacket(packet, data, index, channel);
    }

    /** What {@code buildPacket} returns: Forge mixed ICustomPacket into the vanilla payload packet; here it holds it ({@link #getThis}). */
    private record RawPacket(Packet<?> packet, FriendlyByteBuf data, int index, ResourceLocation name) implements ICustomPacket<Packet<?>> {
        @Override
        public FriendlyByteBuf getInternalData() {
            return data;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public ResourceLocation getName() {
            return name;
        }

        @Override
        public Packet<?> getThis() {
            return packet;
        }
    }
}
