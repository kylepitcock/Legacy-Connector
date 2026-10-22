package org.kylepitcock.mccompat.v1_7_10.entity;

import mccompat.v1_7_10.cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.slf4j.LoggerFactory;

/**
 * Spawn data of legacy hosts: the legacy class name for the fallback types (whose client-side host cannot know it
 * otherwise), then the payload of a legacy {@code IEntityAdditionalSpawnData}, length-prefixed so a mod that reads
 * a different amount than it wrote cannot corrupt the packet.
 */
final class LegacySpawnData {
    private LegacySpawnData() {}

    static void write(LegacyHost host, Entity legacy, RegistryFriendlyByteBuf buf) {
        boolean fallback = LegacyHosts.isFallback(host.mcc$self().getType());
        buf.writeBoolean(fallback);
        if (fallback) buf.writeUtf(legacy == null ? "" : legacy.getClass().getName());
        if (legacy instanceof IEntityAdditionalSpawnData data) {
            var payload = org.kylepitcock.mccompat.core.network.LegacyBuffers.buffer();
            try {
                data.writeSpawnData(payload);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("Retrofit/Entities").warn("{} failed to write its spawn data: {}", legacy.getClass().getName(), e.toString());
                payload.clear();
            }
            buf.writeVarInt(payload.readableBytes());
            buf.writeBytes(payload);
            payload.release();
        } else {
            buf.writeVarInt(0);
        }
    }

    /** Client: returns the legacy object created from the class name for fallback types (else the given one). */
    static Entity read(LegacyHost host, Entity legacy, RegistryFriendlyByteBuf buf) {
        boolean fallback = buf.readBoolean();
        if (fallback) {
            String name = buf.readUtf();
            if (legacy == null && !name.isEmpty()) {
                Class<?> cls = LegacyHosts.loadLegacyClass(name);
                if (cls != null) legacy = LegacyHosts.construct(cls, host, host.mcc$self().level());
            }
        }
        int length = buf.readVarInt();
        var payload = buf.readBytes(length);
        try {
            if (legacy instanceof IEntityAdditionalSpawnData data && length > 0) data.readSpawnData(payload);
        } catch (RuntimeException | LinkageError e) {
            LoggerFactory.getLogger("Retrofit/Entities").warn("{} failed to read its spawn data: {}", legacy.getClass().getName(), e.toString());
        } finally {
            payload.release();
        }
        return legacy;
    }
}
