package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.function.Supplier;

/** {@code HandshakeHandler.HandshakeConsumer}: what a login message consumer looks like (see {@link HandshakeHandler}). */
@FunctionalInterface
public interface HandshakeHandler$HandshakeConsumer<MSG> {
    void accept(HandshakeHandler handler, MSG message, Supplier<NetworkEvent.Context> context);
}
