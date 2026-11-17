package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Forge 1.20.1's login handshake handler: mods register login-phase messages whose consumers run on it. NeoForge 1.21.1 has no login-phase custom
 * messages for these channels and {@code SimpleChannel.MessageBuilder.markAsLoginPacket} never sends them (see there), so no handler instance is ever
 * created; the class exists so that the consumers a mod builds with {@link #indexFirst} and {@link #biConsumerFor} link. The consumers they return do nothing
 * (APPROXIMATION: a mod whose login messages decide something, such as a version check, is not asked).
 */
public class HandshakeHandler {
    public static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> biConsumerFor(HandshakeHandler$HandshakeConsumer<MSG> consumer) {
        return (message, context) -> {
        };
    }

    public static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> indexFirst(HandshakeHandler$HandshakeConsumer<MSG> next) {
        return biConsumerFor(next);
    }

    /** Whether the connection is a vanilla (non-modded) one: never, the loader runs on NeoForge. */
    public boolean isVanilla() {
        return false;
    }
}
