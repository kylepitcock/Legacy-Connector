package legacyconnector.v1_20_1.net.minecraftforge.network.simple;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import legacyconnector.v1_20_1.net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;

/** Forge 1.20.1's per-channel message table; mods only keep the {@link MessageHandler} that {@code registerMessage} returns. */
public final class IndexedMessageCodec {
    private IndexedMessageCodec() {
    }

    /** One registered message type: the mod's encoder, decoder and handler. */
    public static final class MessageHandler<MSG> {
        final int index;
        final Class<MSG> type;
        private final BiConsumer<MSG, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, MSG> decoder;
        private final BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer;

        MessageHandler(int index, Class<MSG> type, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder,
                BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
            this.index = index;
            this.type = type;
            this.encoder = encoder;
            this.decoder = decoder;
            this.consumer = consumer;
        }

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        MSG decode(FriendlyByteBuf buf) {
            return decoder.apply(buf);
        }

        @SuppressWarnings("unchecked")
        void handle(Object message, NetworkEvent.Context context) {
            consumer.accept((MSG) message, () -> context);
        }
    }
}
