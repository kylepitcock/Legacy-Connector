package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.mojang.brigadier.StringReader;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** Regression coverage for Palladium's legacy ParticleAbility deserializer call. */
class ApiCompatParticleDeserializer1201Test {
    @Test
    void simpleParticlesReturnTheirTypeFromTheLegacyCommandParser() throws Exception {
        SimpleParticleType particle = new SimpleParticleType(false);

        LegacyParticleDeserializer<SimpleParticleType> deserializer = ApiCompat.noDeserializer(particle);

        assertNotNull(deserializer);
        assertSame(particle, deserializer.fromCommand(particle, new StringReader("  ")));
    }

    @Test
    void simpleParticlesPreserveTheLegacyNoPayloadNetworkBehavior() {
        SimpleParticleType particle = new SimpleParticleType(false);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        try {
            LegacyParticleDeserializer<SimpleParticleType> deserializer = ApiCompat.noDeserializer(particle);

            assertSame(particle, deserializer.fromNetwork(particle, buffer));
            assertEquals(0, buffer.readableBytes(), "simple particle deserialization must not consume a payload");
        } finally {
            buffer.release();
        }
    }
}
