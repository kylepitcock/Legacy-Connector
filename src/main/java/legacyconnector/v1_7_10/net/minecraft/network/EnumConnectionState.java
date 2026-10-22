package legacyconnector.v1_7_10.net.minecraft.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/** The protocol states; mods (CoFH's security helper) register their packet classes in the id maps. */
public enum EnumConnectionState {
    HANDSHAKING, PLAY, STATUS, LOGIN;

    /** Packet class -> the state it belongs to (mods read this by reflection). */
    public static final java.util.Map<Class, EnumConnectionState> STATES_BY_CLASS = new java.util.HashMap<>();

    private final BiMap<Integer, Class> serverPackets = HashBiMap.create();
    private final BiMap<Integer, Class> clientPackets = HashBiMap.create();

    /** Packets the server sends. */
    public BiMap<Integer, Class> func_150755_b() {
        return serverPackets;
    }

    /** Packets the client sends. */
    public BiMap<Integer, Class> func_150753_a() {
        return clientPackets;
    }
}
