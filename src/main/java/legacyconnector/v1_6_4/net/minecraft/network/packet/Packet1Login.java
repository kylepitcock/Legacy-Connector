package legacyconnector.v1_6_4.net.minecraft.network.packet;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** 1.6.4's login packet; only handed to {@code IConnectionHandler.clientLoggedIn}, with the values of the player's world. */
public class Packet1Login extends Packet {
    public int clientEntityId;
    public int dimension;
    public byte difficultySetting;
    public int maxPlayers;

    public Packet1Login() {}
}
