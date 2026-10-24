package legacyconnector.v1_7_10.net.minecraft.network;

/** Base of the few vanilla packets legacy mods construct directly. */
public abstract class Packet {
    /** Delivers this packet to a player: packets with a modern equivalent send it; the others do nothing. */
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }
}
