package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Stands in for {@code ParticleOptions.Deserializer} (1.20.1). 1.21.1 reads particle options through codecs; a mod's deserializer object is kept for its own
 * code, the particle type is built from the codec it also provides.
 */
public interface LegacyParticleDeserializer<T extends ParticleOptions> {
    T fromCommand(ParticleType<T> type, StringReader reader) throws CommandSyntaxException;

    T fromNetwork(ParticleType<T> type, FriendlyByteBuf buffer);
}
