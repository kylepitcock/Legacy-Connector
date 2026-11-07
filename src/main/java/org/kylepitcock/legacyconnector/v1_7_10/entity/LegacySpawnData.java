package org.kylepitcock.legacyconnector.v1_7_10.entity;

import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
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
            var payload = org.kylepitcock.legacyconnector.core.network.LegacyBuffers.buffer();
            try {
                if (!writeGuava(legacy, payload)) data.writeSpawnData(payload);
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
            if (legacy instanceof IEntityAdditionalSpawnData data && length > 0 && !readGuava(legacy, payload)) data.readSpawnData(payload);
        } catch (RuntimeException | LinkageError e) {
            LoggerFactory.getLogger("LegacyConnector/Entities").warn("{} failed to read its spawn data: {}", legacy.getClass().getName(), e.toString());
        } finally {
            payload.release();
        }
        return legacy;
    }

    // 1.6.4 and older (Forge 6 to 9) took Guava's ByteArrayDataOutput/Input in the two methods; the shim interface takes a ByteBuf, so such a mod's
    // class does not implement it (calling it is an AbstractMethodError). The old form is found on the class and called with Guava streams.

    private static java.lang.reflect.Method guavaMethod(Class<?> type, String name, Class<?> arg) {
        for (Class<?> k = type; k != null && k != Object.class; k = k.getSuperclass()) {
            try {
                var m = k.getDeclaredMethod(name, arg);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // keep looking up the hierarchy
            } catch (LinkageError e) {
                return null;
            }
        }
        return null;
    }

    private static boolean writeGuava(Entity legacy, io.netty.buffer.ByteBuf payload) {
        var m = guavaMethod(legacy.getClass(), "writeSpawnData", com.google.common.io.ByteArrayDataOutput.class);
        if (m == null) return false;
        com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
        try {
            m.invoke(legacy, out);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e.getCause() != null ? e.getCause() : e);
        }
        payload.writeBytes(out.toByteArray());
        return true;
    }

    private static boolean readGuava(Entity legacy, io.netty.buffer.ByteBuf payload) {
        var m = guavaMethod(legacy.getClass(), "readSpawnData", com.google.common.io.ByteArrayDataInput.class);
        if (m == null) return false;
        byte[] bytes = new byte[payload.readableBytes()];
        payload.readBytes(bytes);
        try {
            m.invoke(legacy, com.google.common.io.ByteStreams.newDataInput(bytes));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e.getCause() != null ? e.getCause() : e);
        }
        return true;
    }
}
