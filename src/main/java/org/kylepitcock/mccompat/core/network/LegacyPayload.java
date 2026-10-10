package org.kylepitcock.mccompat.core.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Carrier for every legacy network message: which legacy family and channel it belongs to, the legacy
 * discriminator, and the raw bytes the legacy message serialized itself into.
 */
public record LegacyPayload(String version, String channel, int discriminator, byte[] data) implements CustomPacketPayload {
    public static final Type<LegacyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("mccompat", "legacy"));
    public static final StreamCodec<ByteBuf, LegacyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LegacyPayload::version,
            ByteBufCodecs.STRING_UTF8, LegacyPayload::channel,
            ByteBufCodecs.VAR_INT, LegacyPayload::discriminator,
            ByteBufCodecs.BYTE_ARRAY, LegacyPayload::data,
            LegacyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
