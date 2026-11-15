package legacyconnector.vb1_7_3.net.minecraft.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Beta's {@code Packet}: the base of every network packet class. Mods hand packets around by this type (a tile entity's
 * {@code getDescriptionPacket} returns a {@code Packet230ModLoader} as a {@code Packet}), and register their own packet classes by id
 * ({@code Packet.addIdClassMapping}); such a packet is carried by the layer's channel in its own serialized form.
 */
public abstract class Packet {
    private static final Map<Integer, Class<?>> BY_ID = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Integer> IDS = new ConcurrentHashMap<>();

    /** Beta's {@code isChunkDataPacket}: sent with the world's packets. */
    public boolean worldPacket;

    protected Packet() {}

    /** Beta's {@code addIdClassMapping(id, client, server, class)}. */
    public static void register(int id, boolean client, boolean server, Class<?> type) {
        if (type == null) return;
        BY_ID.put(id, type);
        IDS.put(type, id);
    }

    /** A new packet of a registered id, or null. */
    public static Packet create(int id) {
        Class<?> type = BY_ID.get(id);
        if (type == null) return null;
        try {
            return (Packet) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Beta's {@code getPacketId}: the registered id of this packet's class, -1 if none. */
    public final int getRawId() {
        Integer id = IDS.get(getClass());
        return id == null ? -1 : id;
    }

    /** Beta's {@code readPacketData}. */
    public void read(DataInputStream in) throws IOException {}

    /** Beta's {@code writePacketData}. */
    public void write(DataOutputStream out) throws IOException {}

    /** Beta's {@code processPacket}. */
    public void apply(legacyconnector.vb1_7_3.net.minecraft.network.NetworkHandler handler) {}

    /** Beta's {@code getPacketSize}. */
    public int size() { return 0; }
}
