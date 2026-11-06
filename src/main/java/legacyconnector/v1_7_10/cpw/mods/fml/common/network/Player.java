package legacyconnector.v1_7_10.cpw.mods.fml.common.network;

/**
 * FML 1.6's marker interface for the player a packet handler is given ({@code IPacketHandler.onPacketData(..., Player)}):
 * every player implements it, so mods cast between it and {@code EntityPlayer}. 1.7.10 itself has no such type; it lives
 * in the shared tree only because {@code EntityPlayer} has to implement it.
 */
public interface Player {
}
