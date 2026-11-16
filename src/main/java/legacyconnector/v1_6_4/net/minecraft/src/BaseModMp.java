package legacyconnector.v1_6_4.net.minecraft.src;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;

/**
 * ModLoaderMP's {@code BaseModMp}: a mod with a client and a server half that talk through {@link Packet230ModLoader}. The server half's
 * {@code handlePacket(packet, player)} gets the packets the client half sent with {@link ModLoaderMp#sendPacket}, the client half's
 * {@code handlePacket(packet)} the ones the server half sent with {@link ModLoaderMp#sendPacketTo} / {@link ModLoaderMp#sendPacketToAll}.
 */
public abstract class BaseModMp extends BaseMod {
    public BaseModMp() {}

    /** ModLoaderMP's id of a mod: the hash of its class name, the same in every session. */
    public final int getId() {
        return getClass().getSimpleName().hashCode();
    }

    /** A packet the server sent to this client. */
    public void handlePacket(Packet230ModLoader packet) {}

    /** A packet a client sent to this server. */
    public void handlePacket(Packet230ModLoader packet, EntityPlayerMP source) {}

    public void handleTileEntityPacket(int x, int y, int z, int type, int[] ints, float[] floats, String[] strings) {}

    /** The GUI of a container the server opened for this mod ({@code ModLoaderMp.registerGUI}); client side. */
    public legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen handleGUI(int guiId) {
        return null;
    }

    /** A player logged in to the server running this mod (server side). */
    public void handleLogin(EntityPlayerMP player) {}

    /** The client pressed a key the mod sent with {@code ModLoaderMp.sendKey}. */
    public void handleSendKey(EntityPlayerMP player, int key) {}

    public boolean hasClientSide() {
        return true;
    }

    /** A player joined the server running this mod. */
    public void onClientLogin(EntityPlayer player) {}

    /** A client left the server (the connection is the player's). */
    public void onClientLogout(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network) {}
}
