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

    /** 1.12's reverse table of the packet classes (mods that register packets by reflection fill it); it holds what mods put in it. */
    public static final java.util.Map<Class<? extends Packet<?>>, EnumConnectionState> STATES_BY_CLASS = new java.util.concurrent.ConcurrentHashMap<>();
    /** 1.12's per-direction packet id tables; they hold what mods put in them (the game's registries do the real work). */
    private final java.util.Map<EnumPacketDirection, com.google.common.collect.BiMap<Integer, Class<? extends Packet<?>>>> directionMaps =
            new java.util.EnumMap<>(EnumPacketDirection.class);
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
        EnumConnectionState registered = STATES_BY_CLASS.get(packet.getClass());
        if (registered != null) return registered;
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
        var map = directionMaps.get(direction);
        Class<? extends Packet<?>> type = map == null ? null : map.get(packetId);
        if (type == null) return null;
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
