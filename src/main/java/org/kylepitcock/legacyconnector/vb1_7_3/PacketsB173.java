package org.kylepitcock.legacyconnector.vb1_7_3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import legacyconnector.vb1_7_3.net.minecraft.network.NetworkHandler;
import legacyconnector.vb1_7_3.net.minecraft.network.packet.Packet;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mod's own Beta packet class (registered with {@code Packet.addIdClassMapping}) sent from the client ({@code addToSendQueue}): its
 * {@code writePacketData} bytes travel on the layer's channel, and the server makes the packet of that id, reads it and runs its
 * {@code processPacket} with the sending player as the callback's player. Only registered ids are made.
 */
public final class PacketsB173 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final String FAMILY = "b1.7.3";
    private static final int MAX_BYTES = 1 << 20;
    private static final java.util.Set<Integer> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final NetworkHandler SERVER = new NetworkHandler() {
        @Override public boolean isServerSide() { return true; }
    };
    private static boolean installed;

    private PacketsB173() {}

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        LegacyNetwork.registerReceiver(FAMILY, PacketsB173::receive);
    }

    /** The client's {@code addToSendQueue}. */
    public static void toServer(Packet packet) {
        if (packet == null) return;
        int id = packet.getRawId();
        if (id < 0) {
            if (REPORTED.add(-1)) LOG.warn("b1.7.3 packet {} has no registered id; it is not sent", packet.getClass().getName());
            return;
        }
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return;
        byte[] data;
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) { packet.write(out); }
            data = bytes.toByteArray();
        } catch (IOException | RuntimeException e) {
            LOG.warn("b1.7.3 packet {} could not be written: {}", packet.getClass().getName(), e.toString());
            return;
        }
        if (data.length > MAX_BYTES) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new LegacyPayload(FAMILY, "packet", id, data));
    }

    private static void receive(LegacyPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!"packet".equals(payload.channel()) || !(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (payload.data().length > MAX_BYTES) return;
        context.enqueueWork(() -> {
            Packet packet = Packet.create(payload.discriminator());
            if (packet == null) return;
            try {
                packet.read(new DataInputStream(new ByteArrayInputStream(payload.data())));
            } catch (IOException | RuntimeException e) {
                if (REPORTED.add(payload.discriminator())) LOG.warn("b1.7.3 packet {} could not be read: {}", packet.getClass().getName(), e.toString());
                return;
            }
            var view = PlayerViewB173.of(player);
            World world = World.mcc$wrap(player.level());
            try (var views = EntityViewsB173.callbackScope();
                 var minecraft = legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world, view)) {
                packet.apply(SERVER);
                view.writeBack();
            } catch (RuntimeException | LinkageError e) {
                if (REPORTED.add(payload.discriminator())) LOG.warn("b1.7.3 packet {} failed on the server", packet.getClass().getName(), e);
            } finally {
                EntityViewsB173.flush();
                BlockEntityViewsB173.flush();
            }
        });
    }
}
