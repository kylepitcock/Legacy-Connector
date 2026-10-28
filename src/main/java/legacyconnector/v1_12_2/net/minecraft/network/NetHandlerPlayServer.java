package legacyconnector.v1_12_2.net.minecraft.network;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/** A player's connection on the server: a view over the modern player's connection. */
public class NetHandlerPlayServer implements legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer {
    public EntityPlayerMP player;

    /** Phantom fields: accesses are redirected to the accessors below. */
    @Deprecated
    public NetworkManager netManager;
    @Deprecated
    public int floatingTickCount;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.player = player;
    }

    private net.minecraft.server.network.ServerGamePacketListenerImpl modern() {
        var p = player == null ? null : player.mcc$serverPlayer();
        return p == null ? null : p.connection;
    }

    public static NetworkManager mcc$get$netManager(NetHandlerPlayServer h) {
        return h.getNetworkManager();
    }

    private static java.lang.reflect.Field aboveGround;

    private static java.lang.reflect.Field aboveGroundField() throws ReflectiveOperationException {
        if (aboveGround == null) {
            java.lang.reflect.Field f = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
            f.setAccessible(true);
            aboveGround = f;
        }
        return aboveGround;
    }

    /** How long the player has been in the air without flying (the server kicks for flying at 80). */
    public static int mcc$get$floatingTickCount(NetHandlerPlayServer h) {
        var m = h.modern();
        try {
            return m == null ? 0 : aboveGroundField().getInt(m);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    public static void mcc$set$floatingTickCount(NetHandlerPlayServer h, int value) {
        var m = h.modern();
        try {
            if (m != null) aboveGroundField().setInt(m, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // the kick timer stays as it is
        }
    }

    /** The manager a mod passed in (fake players' handlers), else none. */
    private NetworkManager mcc$manager;

    /** The handler mods build for their fake players: it becomes the player's {@code connection}. */
    public NetHandlerPlayServer(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server, NetworkManager networkManager, EntityPlayerMP player) {
        this(player);
        this.mcc$manager = networkManager;
        if (player != null) EntityPlayerMP.mcc$set$connection(player, this);
    }

    /** Resets the anti-cheat's last good position to where the player is (after a mod moved it). */
    public void captureCurrentPosition() {
        var m = modern();
        if (m != null) m.resetPosition();
    }

    public NetworkManager getNetworkManager() {
        if (mcc$manager != null) return mcc$manager;
        var m = modern();
        return m == null ? null : NetworkManager.mcc$of(m.getConnection());
    }

    /** Sends a vanilla packet a mod built, as the modern packet it describes. */
    public void sendPacket(Packet<?> packetIn) {
        var m = modern();
        var packet = packetIn == null ? null : packetIn.mcc$modern();
        if (m != null && packet != null) m.send(packet);
    }

    public void disconnect(ITextComponent textComponent) {
        var m = modern();
        if (m != null) m.disconnect(textComponent == null ? net.minecraft.network.chat.Component.empty() : textComponent.mcc$modern());
    }

    @Override
    public void onDisconnect(ITextComponent reason) {
        disconnect(reason);
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        var m = modern();
        if (m != null) m.teleport(x, y, z, yaw, pitch);
    }

    /** {@code relativeSet} holds SPacketPlayerPosLook.EnumFlags: X, Y, Z, Y_ROT, X_ROT, the parts that are relative. */
    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch, java.util.Set<?> relativeSet) {
        var m = modern();
        if (m == null) return;
        java.util.Set<net.minecraft.world.entity.RelativeMovement> relative = java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class);
        if (relativeSet != null) {
            for (Object o : relativeSet) {
                if (o instanceof Enum<?> e) {
                    try {
                        relative.add(net.minecraft.world.entity.RelativeMovement.valueOf(e.name()));
                    } catch (IllegalArgumentException ignored) {
                        // not a movement flag
                    }
                }
            }
        }
        m.teleport(x, y, z, yaw, pitch, relative);
    }

    /** Handles a digging packet a mod builds for its player (as the game's player action). */
    public void processPlayerDigging(legacyconnector.v1_12_2.net.minecraft.network.play.client.CPacketPlayerDigging packet) {
        var p = player == null ? null : player.mcc$serverPlayer();
        if (p == null || packet == null || p.connection == null) return;
        p.connection.handlePlayerAction(packet.mcc$modern());
    }
}
