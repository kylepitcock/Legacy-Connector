package legacyconnector.v1_12_2.net.minecraft.network;

/**
 * The connection protocols (handshaking, play, status, login) with their 1.12 ids. The game's own packet registries replace 1.12's per-state
 * packet id tables: a legacy packet's id is a stable number for the modern packet type it is sent as, and ids map back to no legacy packet.
 */
public enum EnumConnectionState {
    HANDSHAKING(-1),
    PLAY(0),
    STATUS(1),
    LOGIN(2);

    private final int id;

    EnumConnectionState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static EnumConnectionState getById(int id) {
        for (EnumConnectionState s : values()) if (s.id == id) return s;
        return null;
    }

    /** The protocol a packet belongs to (vanilla play packets and mod payloads: play). */
    public static EnumConnectionState getFromPacket(Packet<?> packet) {
        if (packet == null) return null;
        String name = packet.getClass().getName();
        if (name.contains(".network.login.")) return LOGIN;
        if (name.contains(".network.status.")) return STATUS;
        if (name.contains(".network.handshake.")) return HANDSHAKING;
        return PLAY;
    }

    /** A stable number for the modern packet type a legacy one is sent as (the hash code of its type id), or null when it has none. */
    public Integer getPacketId(EnumPacketDirection direction, Packet<?> packet) {
        var modern = packet == null ? null : packet.mcc$modern();
        if (modern == null) return null;
        return modern.type().id().hashCode();
    }

    /** Legacy packets are not created from ids any more. */
    public Packet<?> getPacket(EnumPacketDirection direction, int packetId) {
        return null;
    }
}
