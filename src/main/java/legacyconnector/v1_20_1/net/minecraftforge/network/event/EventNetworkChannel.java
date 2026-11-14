package legacyconnector.v1_20_1.net.minecraftforge.network.event;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import legacyconnector.v1_20_1.net.minecraftforge.network.NetworkEvent;
import legacyconnector.v1_20_1.net.minecraftforge.network.RawChannels;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Forge 1.20.1's raw event channel ({@code NetworkRegistry.newEventChannel}): a mod listens to the bytes of a channel by hand and builds its packets with
 * {@code NetworkDirection.buildPacket}. Over NeoForge 1.21.1: one payload type per channel name that carries the raw bytes in both directions; a received
 * payload is posted on the channel's own event bus as {@link NetworkEvent.ServerCustomPayloadEvent} (serverbound) or
 * {@link NetworkEvent.ClientCustomPayloadEvent} (clientbound), on the main thread (NeoForge runs payload handlers there; a listener that wants the old
 * network thread's timing needs nothing more, {@code enqueueWork} runs at once on the main thread).
 */
public class EventNetworkChannel {
    private static final List<EventNetworkChannel> CHANNELS = new ArrayList<>();

    private final ResourceLocation name;
    private final String version;
    private final CustomPacketPayload.Type<Payload> type;
    private final IEventBus bus = BusBuilder.builder().build();
    private boolean registered;

    public EventNetworkChannel(ResourceLocation name) {
        this(name, () -> "1");
    }

    public EventNetworkChannel(ResourceLocation name, Supplier<String> version) {
        this.name = name;
        this.version = version.get();
        this.type = new CustomPacketPayload.Type<>(name);
        RawChannels.register(name, (data, index) -> {
            byte[] bytes = new byte[data.readableBytes()];
            data.getBytes(data.readerIndex(), bytes);
            return new Payload(bytes);
        });
        synchronized (CHANNELS) {
            CHANNELS.add(this);
        }
    }

    public ResourceLocation getName() {
        return name;
    }

    /** The raw bytes of one packet. */
    public final class Payload implements CustomPacketPayload {
        final byte[] data;

        Payload(byte[] data) {
            this.data = data;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return type;
        }
    }

    /** Called by the runtime on {@link RegisterPayloadHandlersEvent}: registers every event channel created so far. */
    public static void registerAll(RegisterPayloadHandlersEvent event) {
        List<EventNetworkChannel> channels;
        synchronized (CHANNELS) {
            channels = new ArrayList<>(CHANNELS);
        }
        for (EventNetworkChannel c : channels) c.register(event);
    }

    private void register(RegisterPayloadHandlersEvent event) {
        if (registered) return;
        registered = true;
        StreamCodec<RegistryFriendlyByteBuf, Payload> codec = StreamCodec.of((buf, p) -> buf.writeBytes(p.data), buf -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new Payload(bytes);
        });
        event.registrar(version).optional().playBidirectional(type, codec, this::handle);
    }

    private void handle(Payload payload, IPayloadContext ctx) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data));
        NetworkEvent.Context context = new NetworkEvent.Context(ctx);
        bus.post(ctx.flow().isServerbound() ? new NetworkEvent.ServerCustomPayloadEvent(buf, context) : new NetworkEvent.ClientCustomPayloadEvent(buf, context));
    }

    /** {@code registerObject(this)}: the {@code @SubscribeEvent} methods of the object listen to the channel's events. */
    public void registerObject(Object target) {
        bus.register(target);
    }

    /** {@code addListener(this::onPacket)}: the listener's event type (client or server payload event) selects the packets it gets. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void addListener(Consumer<T> eventListener) {
        bus.addListener((Consumer) eventListener);
    }

    /** Login-phase gathering is not part of NeoForge's play payloads: the listener is accepted and never called. */
    public <T> void addGatherListener(Consumer<T> eventListener) {
    }

    public boolean isRemotePresent(net.minecraft.network.Connection manager) {
        return true;
    }
}
