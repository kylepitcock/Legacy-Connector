package org.kylepitcock.legacyconnector.v1_20_1.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.core.network.LegacyBuffers;


class RegistryFriendlyBuffers1201Test {
    @Test
    void heapBufferCarriesRegistryAndNeoForgeConnectionContext() {
        ByteBuf raw = LegacyBuffers.buffer();
        try {
            RegistryFriendlyByteBuf buffer = RegistryFriendlyBuffers1201.wrap(raw, RegistryAccess.EMPTY);
            assertTrue(buffer.hasArray(), "legacy packet serializers require an accessible heap array");
            assertEquals(ConnectionType.NEOFORGE, buffer.getConnectionType());
            assertNotNull(buffer.registryAccess());
            assertEquals(RegistryAccess.EMPTY, buffer.registryAccess());
        } finally {
            raw.release();
        }
    }
    // ItemStackCodec1201 round-trip tests need a bootstrapped Minecraft (language files), which the root unit-test classpath lacks;
    // the fork ran them with a build.gradle change that is not taken here.
}
