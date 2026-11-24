package legacyconnector.v1_12_2.mezz.jei.network.packets;

/**
 * JEI 4 (1.12.2) internal base of its packets. Legacy plugins (recipe transfer handlers) build a packet and hand it to JEI's proxy, which here
 * has no server counterpart: the class exists so the references link and type-check.
 */
public abstract class PacketJei {
}
