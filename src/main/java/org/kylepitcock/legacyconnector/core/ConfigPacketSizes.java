package org.kylepitcock.legacyconnector.core;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.resources.RegistryOps;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the encoded size of the registry packets a joining client receives in the configuration phase. A frame over 2 MiB (21 bits of
 * length) breaks the client's frame decoder ("CorruptedFrameException: length wider than 21-bit", reported when Connectivity on the
 * server let a larger frame through); legacy content grows the biome registry, which carries every biome's feature list.
 */
public final class ConfigPacketSizes {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Network");
    private static final int FRAME_LIMIT = (1 << 21) - 1;

    private ConfigPacketSizes() {}

    public static void register(IEventBus game) {
        game.addListener((ServerStartedEvent e) -> {
            try {
                report(e.getServer());
            } catch (RuntimeException | LinkageError ex) {
                LOG.debug("Could not measure the configuration packets", ex);
            }
        });
    }

    private record Size(String name, int bytes) {}

    private static void report(net.minecraft.server.MinecraftServer server) {
        var access = server.registryAccess();
        var ops = RegistryOps.create(NbtOps.INSTANCE, access);
        List<Size> sizes = new ArrayList<>();
        RegistrySynchronization.packRegistries(ops, access, Set.of(), (key, entries) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                ClientboundRegistryDataPacket.STREAM_CODEC.encode(buf, new ClientboundRegistryDataPacket(key, entries));
                sizes.add(new Size(key.location().toString(), buf.readableBytes()));
            } finally {
                buf.release();
            }
        });
        sizes.sort(Comparator.comparingInt(Size::bytes).reversed());
        long total = 0;
        for (Size s : sizes) total += s.bytes;
        StringBuilder top = new StringBuilder();
        for (int i = 0; i < Math.min(4, sizes.size()); i++) top.append(i == 0 ? "" : ", ").append(sizes.get(i).name).append(' ').append(sizes.get(i).bytes >> 10).append(" KiB");
        LOG.info("Configuration registry packets: {} registries, {} KiB uncompressed; largest: {}", sizes.size(), total >> 10, top);
        for (Size s : sizes) {
            if (s.bytes > FRAME_LIMIT / 2) {
                LOG.warn("The {} registry packet is {} KiB (a network frame holds at most {} KiB): clients may fail to join with \"length wider than 21-bit\"",
                        s.name, s.bytes >> 10, FRAME_LIMIT >> 10);
            }
        }
    }
}
