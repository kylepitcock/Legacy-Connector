package legacyconnector.v1_20_1.net.minecraftforge.network.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import legacyconnector.v1_20_1.net.minecraftforge.network.NetworkDirection;
import legacyconnector.v1_20_1.net.minecraftforge.network.NetworkEvent;
import legacyconnector.v1_20_1.net.minecraftforge.network.PacketDistributor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Forge 1.20.1's {@code SimpleChannel} over NeoForge 1.21.1's payload system. A channel is one payload type (the channel's name as its id); a message is
 * the message index plus whatever the mod's own encoder writes, so the mod's {@code encode}/{@code decode} methods work unchanged. The payload types are
 * registered when NeoForge fires {@link RegisterPayloadHandlersEvent} ({@link #registerAll}); messages may be added to a channel until then and after.
 */
public class SimpleChannel {
    private static final List<SimpleChannel> CHANNELS = new ArrayList<>();

    private final ResourceLocation name;
    private final String version;
    private final CustomPacketPayload.Type<Payload> type;
    private final Map<Integer, IndexedMessageCodec.MessageHandler<?>> byIndex = new ConcurrentHashMap<>();
    private final Map<Class<?>, IndexedMessageCodec.MessageHandler<?>> byClass = new ConcurrentHashMap<>();

    public SimpleChannel(ResourceLocation name, Supplier<String> version, Predicate<String> clientAccepts, Predicate<String> serverAccepts) {
        this.name = name;
        this.version = version.get();
        this.type = new CustomPacketPayload.Type<>(name);
        // NetworkDirection.buildPacket: bytes a mod encoded itself (with encodeMessage) become a payload of this channel
        legacyconnector.v1_20_1.net.minecraftforge.network.RawChannels.register(name, (data, index) -> {
            byte[] bytes = new byte[data.readableBytes()];
            data.getBytes(data.readerIndex(), bytes);
            return new Payload(index, bytes);
        });
        synchronized (CHANNELS) {
            CHANNELS.add(this);
        }
    }

    public ResourceLocation getName() {
        return name;
    }

    // ---- registration with NeoForge ---------------------------------------------------------------------------------------

    /** The payload of every message of the channel. */
    public final class Payload implements CustomPacketPayload {
        final IndexedMessageCodec.MessageHandler<?> handler;
        final Object message;
        /** A message the mod encoded itself ({@code buildPacket}): index and bytes are written as they are. */
        final byte[] raw;
        final int rawIndex;

        Payload(IndexedMessageCodec.MessageHandler<?> handler, Object message) {
            this.handler = handler;
            this.message = message;
            this.raw = null;
            this.rawIndex = 0;
        }

        Payload(int index, byte[] raw) {
            this.handler = null;
            this.message = null;
            this.raw = raw;
            this.rawIndex = index;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return type;
        }
    }

    /** Called by the runtime on {@link RegisterPayloadHandlersEvent}: registers every channel created so far. */
    public static void registerAll(RegisterPayloadHandlersEvent event) {
        List<SimpleChannel> channels;
        synchronized (CHANNELS) {
            channels = new ArrayList<>(CHANNELS);
        }
        for (SimpleChannel c : channels) c.register(event);
    }

    private boolean registered;

    private void register(RegisterPayloadHandlersEvent event) {
        if (registered) return;
        registered = true;
        StreamCodec<RegistryFriendlyByteBuf, Payload> codec = StreamCodec.of(this::encode, this::decode);
        event.registrar(version).optional().playBidirectional(type, codec, this::handle);
    }

    private void encode(RegistryFriendlyByteBuf buf, Payload payload) {
        if (payload.raw != null) {
            buf.writeByte(payload.rawIndex);
            buf.writeBytes(payload.raw);
            return;
        }
        buf.writeByte(payload.handler.index);
        payload.handler.encode(payload.message, buf);
    }

    private Payload decode(RegistryFriendlyByteBuf buf) {
        int index = buf.readByte() & 0xFF;
        IndexedMessageCodec.MessageHandler<?> h = byIndex.get(index);
        if (h == null) throw new IllegalStateException("Unknown message index " + index + " on channel " + name);
        return new Payload(h, h.decode(buf));
    }

    private void handle(Payload payload, IPayloadContext ctx) {
        payload.handler.handle(payload.message, new NetworkEvent.Context(ctx));
    }

    // ---- Forge's API --------------------------------------------------------------------------------------------------------

    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
        return registerMessage(index, messageType, encoder, decoder, consumer, Optional.empty());
    }

    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer, Optional<NetworkDirection> direction) {
        IndexedMessageCodec.MessageHandler<MSG> h = new IndexedMessageCodec.MessageHandler<>(index, messageType, encoder, decoder, consumer);
        byIndex.put(index, h);
        byClass.put(messageType, h);
        return h;
    }

    public <MSG> MessageBuilder<MSG> messageBuilder(Class<MSG> type, int id) {
        return new MessageBuilder<>(this, type, id);
    }

    public <MSG> MessageBuilder<MSG> messageBuilder(Class<MSG> type, int id, NetworkDirection direction) {
        return new MessageBuilder<>(this, type, id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Payload wrap(Object msg) {
        IndexedMessageCodec.MessageHandler<?> h = byClass.get(msg.getClass());
        if (h == null) throw new IllegalArgumentException("Message " + msg.getClass().getName() + " is not registered on channel " + name);
        return new Payload(h, msg);
    }

    /** {@code channel.encodeMessage(msg, buffer)}: writes the message the way {@code send} would and returns its index (for {@code NetworkDirection.buildPacket}). */
    public <MSG> int encodeMessage(MSG message, FriendlyByteBuf buf) {
        IndexedMessageCodec.MessageHandler<?> h = byClass.get(message.getClass());
        if (h == null) throw new IllegalArgumentException("Message " + message.getClass().getName() + " is not registered on channel " + name);
        h.encode(message, buf);
        return h.index;
    }

    public void sendToServer(Object message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(wrap(message));
    }

    public void send(PacketDistributor.PacketTarget target, Object message) {
        target.send(wrap(message));
    }

    /** {@code channel.sendTo(msg, connection, direction)}: straight to one connection. */
    public <MSG> void sendTo(MSG message, net.minecraft.network.Connection manager, NetworkDirection direction) {
        Payload payload = wrap(message);
        manager.send(direction == NetworkDirection.PLAY_TO_SERVER || direction == NetworkDirection.LOGIN_TO_SERVER
                ? new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(payload)
                : new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(payload));
    }

    public <MSG> void reply(MSG message, NetworkEvent.Context context) {
        context.mcc$delegate().reply(wrap(message));
    }

    public net.minecraft.network.protocol.Packet<?> toVanillaPacket(Object message, NetworkDirection direction) {
        return new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(wrap(message));
    }

    public boolean isRemotePresent(net.minecraft.network.Connection manager) {
        return true;
    }

    /** {@code channel.messageBuilder(Msg.class, id).encoder(..).decoder(..).consumerMainThread(..).add()}. */
    public static final class MessageBuilder<MSG> {
        private final SimpleChannel channel;
        private final Class<MSG> type;
        private final int id;
        private BiConsumer<MSG, FriendlyByteBuf> encoder;
        private Function<FriendlyByteBuf, MSG> decoder;
        private BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer;

        MessageBuilder(SimpleChannel channel, Class<MSG> type, int id) {
            this.channel = channel;
            this.type = type;
            this.id = id;
        }

        public MessageBuilder<MSG> encoder(BiConsumer<MSG, FriendlyByteBuf> encoder) {
            this.encoder = encoder;
            return this;
        }

        public MessageBuilder<MSG> decoder(Function<FriendlyByteBuf, MSG> decoder) {
            this.decoder = decoder;
            return this;
        }

        public MessageBuilder<MSG> consumerNetworkThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
            this.consumer = consumer;
            return this;
        }

        public MessageBuilder<MSG> consumerMainThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
            this.consumer = consumer;
            return this;
        }

        /** Forge's boolean-returning handler form: the result says whether the packet was handled (it always is here). */
        @FunctionalInterface
        public interface ToBooleanBiFunction<A, B> {
            boolean apply(A a, B b);
        }

        public MessageBuilder<MSG> consumerNetworkThread(ToBooleanBiFunction<MSG, Supplier<NetworkEvent.Context>> handler) {
            this.consumer = (msg, ctx) -> {
                if (handler.apply(msg, ctx)) ctx.get().setPacketHandled(true);
            };
            return this;
        }

        /** Login-phase messages are not part of NeoForge's play payloads: the message is registered like any other and never sent at login. */
        public MessageBuilder<MSG> markAsLoginPacket() {
            return this;
        }

        public MessageBuilder<MSG> loginIndex(Function<MSG, Integer> getter, BiConsumer<MSG, Integer> setter) {
            return this;
        }

        public MessageBuilder<MSG> buildLoginPacketList(Function<Boolean, List<com.mojang.datafixers.util.Pair<String, MSG>>> generator) {
            return this;
        }

        public MessageBuilder<MSG> noResponse() {
            return this;
        }

        public void add() {
            channel.registerMessage(id, type, encoder, decoder, consumer);
        }
    }
}
