package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.LoggerFactory;

/**
 * The legacy {@link EntityDataManager} of a hosted entity travels to clients inside one modern synced value (a compound holding the
 * legacy wire form of every entry), so mod entities' renderers on the client see the values their server side sets.
 */
final class DataSync1122 {
    private DataSync1122() {}

    private static final java.util.Set<Class<?>> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** All entries of the legacy data manager, or null when it has none or cannot be written. */
    static CompoundTag encode(Entity legacy) {
        EntityDataManager dm = legacy.getDataManager();
        if (dm == null || dm.isEmpty()) return null;
        var raw = io.netty.buffer.Unpooled.buffer();
        try {
            dm.writeEntries(new PacketBuffer(raw));
            byte[] bytes = new byte[raw.readableBytes()];
            raw.readBytes(bytes);
            CompoundTag tag = new CompoundTag();
            tag.putByteArray("d", bytes);
            return tag;
        } catch (Exception | LinkageError e) {
            if (FAILED.add(legacy.getClass())) {
                LoggerFactory.getLogger("LegacyConnector/Entities").warn("Cannot send the data of {} to clients: {}", legacy.getClass().getName(), e.toString());
            }
            return null;
        } finally {
            raw.release();
        }
    }

    /** Server, every tick: the encoded entries when the legacy data changed since the last call (else null). */
    static CompoundTag changed(Entity legacy) {
        EntityDataManager dm = legacy.getDataManager();
        if (dm == null || !dm.isDirty()) return null;
        dm.setClean();
        return encode(legacy);
    }

    /** Client: applies values received from the server and tells the legacy entity which keys changed. */
    static void apply(Entity legacy, CompoundTag tag) {
        if (tag == null || !tag.contains("d")) return;
        EntityDataManager dm = legacy.getDataManager();
        if (dm == null) return;
        var raw = io.netty.buffer.Unpooled.wrappedBuffer(tag.getByteArray("d"));
        try {
            var entries = EntityDataManager.readEntries(new PacketBuffer(raw));
            if (entries == null) return;
            dm.setEntryValues(entries);
            for (var e : entries) {
                try {
                    legacy.notifyDataManagerChange(e.getKey());
                } catch (RuntimeException | LinkageError ex) {
                    // a mod reacting to a value it cannot handle yet
                }
            }
        } catch (Exception | LinkageError e) {
            if (FAILED.add(legacy.getClass())) {
                LoggerFactory.getLogger("LegacyConnector/Entities").warn("Cannot read the data of {} from the server: {}", legacy.getClass().getName(), e.toString());
            }
        } finally {
            raw.release();
        }
    }
}
