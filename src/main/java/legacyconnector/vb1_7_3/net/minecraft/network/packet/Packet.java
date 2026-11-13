package legacyconnector.vb1_7_3.net.minecraft.network.packet;

/**
 * Beta's {@code Packet}: the base of every network packet class. Mods hand packets around by this type (a tile entity's
 * {@code getDescriptionPacket} returns a {@code Packet230ModLoader} as a {@code Packet}); the wire format is the layer's own.
 */
public abstract class Packet {
    protected Packet() {}
}
