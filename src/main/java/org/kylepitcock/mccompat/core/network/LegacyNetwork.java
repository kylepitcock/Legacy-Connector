package org.kylepitcock.mccompat.core.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.LoggerFactory;

/** Routes {@link LegacyPayload}s to the legacy family that owns the channel. */
public final class LegacyNetwork {
    private static final Map<String, BiConsumer<LegacyPayload, IPayloadContext>> RECEIVERS = new ConcurrentHashMap<>();

    private LegacyNetwork() {}

    public static void registerReceiver(String version, BiConsumer<LegacyPayload, IPayloadContext> receiver) {
        RECEIVERS.put(version, receiver);
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        // optional(): vanilla clients and servers without the layer can still connect.
        event.registrar("1").optional().playBidirectional(LegacyPayload.TYPE, LegacyPayload.CODEC, LegacyNetwork::receive);
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        var receiver = RECEIVERS.get(payload.version());
        if (receiver == null) {
            LoggerFactory.getLogger("MCCompat/Network").debug("Dropping legacy message for unknown family {}", payload.version());
            return;
        }
        receiver.accept(payload, context);
    }
}
