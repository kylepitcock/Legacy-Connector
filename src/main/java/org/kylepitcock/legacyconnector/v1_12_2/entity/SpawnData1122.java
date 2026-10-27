package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.slf4j.LoggerFactory;

/**
 * Spawn data of 1.12.2 hosts: the legacy class name for the fallback types (whose client-side host cannot know it otherwise), then
 * the payload of a legacy {@code IEntityAdditionalSpawnData}, length-prefixed so a mod that reads a different amount than it wrote
 * cannot corrupt the packet.
 */
public final class SpawnData1122 {
    private SpawnData1122() {}

    static void write(Host1122 host, Entity legacy, RegistryFriendlyByteBuf buf) {
        boolean fallback = Hosts1122.isFallback(host.mcc$self().getType());
        buf.writeBoolean(fallback);
        if (fallback) buf.writeUtf(legacy == null ? "" : legacy.getClass().getName());
        if (legacy instanceof IEntityAdditionalSpawnData data) {
            var payload = io.netty.buffer.Unpooled.buffer();
            try {
                data.writeSpawnData(payload);
            } catch (RuntimeException | LinkageError e) {
                LoggerFactory.getLogger("LegacyConnector/Entities").warn("{} failed to write its spawn data: {}", legacy.getClass().getName(), e.toString());
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
    static Entity read(Host1122 host, Entity legacy, RegistryFriendlyByteBuf buf) {
        boolean fallback = buf.readBoolean();
        if (fallback) {
            String name = buf.readUtf();
            if (legacy == null && !name.isEmpty()) {
                Class<?> cls = Hosts1122.loadLegacyClass(name);
                if (cls != null) legacy = Hosts1122.construct(cls, host, host.mcc$self().level());
            }
        }
        int length = buf.readVarInt();
        var payload = buf.readBytes(length);
        try {
            if (legacy instanceof IEntityAdditionalSpawnData data && length > 0) data.readSpawnData(payload);
        } catch (RuntimeException | LinkageError e) {
            LoggerFactory.getLogger("LegacyConnector/Entities").warn("{} failed to read its spawn data: {}", legacy.getClass().getName(), e.toString());
        } finally {
            payload.release();
        }
        return legacy;
    }

    /** 1.12's group spawn data ({@code IEntityLivingData}) carried through the modern {@code SpawnGroupData} of a spawn group. */
    public record Group(legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData data) implements net.minecraft.world.entity.SpawnGroupData {
        public static legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData legacy(net.minecraft.world.entity.SpawnGroupData modern) {
            return modern instanceof Group g ? g.data() : null;
        }

        public static net.minecraft.world.entity.SpawnGroupData modern(legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData legacy) {
            return legacy == null ? null : new Group(legacy);
        }
    }
}
