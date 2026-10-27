package legacyconnector.v1_12_2.net.minecraft.network;

/** Something that handles a connection's packets (the server's handler of one player, the client's handler). */
public interface INetHandler {
    default void onDisconnect(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent reason) {
    }
}
