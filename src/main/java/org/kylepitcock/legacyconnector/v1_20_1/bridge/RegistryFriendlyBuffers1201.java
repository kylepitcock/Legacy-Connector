package org.kylepitcock.legacyconnector.v1_20_1.bridge;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

/**
 * Creates heap-backed registry-aware buffers for the legacy PLAY transport.
 *
 * <p>The outer LegacyPayload carrier is only negotiated by NeoForge peers, so the
 * buffer's connection context is NEOFORGE on both ends of this bridge.</p>
 */
public final class RegistryFriendlyBuffers1201 {
    private RegistryFriendlyBuffers1201() {}

    public static RegistryFriendlyByteBuf wrap(ByteBuf source, RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(
                Objects.requireNonNull(source, "source"),
                Objects.requireNonNull(registryAccess, "registryAccess"),
                ConnectionType.NEOFORGE);
    }
}
