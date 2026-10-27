package legacyconnector.v1_12_2.net.minecraft.network;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;

/**
 * The server's handler of one player's connection: a view over the modern player's game packet listener. Packets mods send
 * through it become their modern equivalents ({@link Packet#mcc$modern}). A handler built for a fake player (with a detached
 * {@link NetworkManager}) sends nothing.
 */
public class NetHandlerPlayServer implements legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer,
        legacyconnector.v1_12_2.net.minecraft.util.ITickable {
    public EntityPlayerMP player;
    public final NetworkManager netManager;

    /** Phantom field: accesses are redirected to {@code mcc$get$floatingTickCount} (the modern listener's airborne tick count). */
    @Deprecated
    public int floatingTickCount;

    public NetHandlerPlayServer(EntityPlayerMP player) {
        this.player = player;
        var sp = player == null ? null : player.mcc$serverPlayer();
        this.netManager = sp != null && sp.connection != null ? new NetworkManager(sp.connection.getConnection()) : new NetworkManager(EnumPacketDirection.SERVERBOUND);
        this.netManager.setNetHandler(this);
    }

    public NetHandlerPlayServer(legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server, NetworkManager networkManager, EntityPlayerMP player) {
        this.player = player;
        this.netManager = networkManager == null ? new NetworkManager(EnumPacketDirection.SERVERBOUND) : networkManager;
        this.netManager.setNetHandler(this);
        if (player != null) EntityPlayerMP.mcc$set$connection(player, this);
    }

    private net.minecraft.server.network.ServerGamePacketListenerImpl mcc$listener() {
        var sp = player == null ? null : player.mcc$serverPlayer();
        return sp == null ? null : sp.connection;
    }

    public NetworkManager getNetworkManager() {
        return netManager;
    }

    /** Sends the packet's modern form to this player; packets with no modern form, and fake players, send nothing. */
    public void sendPacket(Packet<?> packet) {
        if (packet == null || netManager.mcc$connection == null) return;
        var sp = player == null ? null : player.mcc$serverPlayer();
        var listener = mcc$listener();
        if (sp == null || listener == null) return;
        net.minecraft.network.protocol.Packet<?> modern;
        try {
            modern = packet.mcc$modern(sp);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Network").warn("Cannot send legacy packet {}: {}", packet.getClass().getName(), e.toString());
            return;
        }
        if (modern != null) listener.send(modern);
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        var listener = mcc$listener();
        if (listener != null) listener.teleport(x, y, z, yaw, pitch);
    }

    /** {@code relativeSet} holds {@code SPacketPlayerPosLook.EnumFlags} (X, Y, Z, Y_ROT, X_ROT), the modern relative movements' names. */
    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch, java.util.Set<?> relativeSet) {
        var listener = mcc$listener();
        if (listener == null) return;
        java.util.Set<net.minecraft.world.entity.RelativeMovement> relative = java.util.EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class);
        if (relativeSet != null) {
            for (Object flag : relativeSet) {
                try {
                    relative.add(net.minecraft.world.entity.RelativeMovement.valueOf(String.valueOf(flag instanceof Enum<?> e ? e.name() : flag)));
                } catch (IllegalArgumentException e) {
                    // not a movement flag
                }
            }
        }
        listener.teleport(x, y, z, yaw, pitch, relative);
    }

    public void disconnect(ITextComponent textComponent) {
        var listener = mcc$listener();
        if (listener != null) listener.disconnect(textComponent == null ? net.minecraft.network.chat.Component.empty() : textComponent.mcc$modern());
    }

    /** Resets the anti-cheat's last good position to where the player is (after a mod moved it). */
    public void captureCurrentPosition() {
        var listener = mcc$listener();
        if (listener != null) listener.resetPosition();
    }

    @Override
    public void onDisconnect(ITextComponent reason) {
    }

    /** The modern listener ticks itself. */
    @Override
    public void update() {
    }

    private static java.lang.reflect.Field mcc$floating;

    private static java.lang.reflect.Field mcc$floatingField() {
        if (mcc$floating == null) {
            try {
                var f = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                f.setAccessible(true);
                mcc$floating = f;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return mcc$floating;
    }

    public static int mcc$get$floatingTickCount(NetHandlerPlayServer h) {
        var listener = h.mcc$listener();
        var f = mcc$floatingField();
        try {
            return listener == null || f == null ? 0 : f.getInt(listener);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    /** Mods reset it to keep a flying player from being kicked. */
    public static void mcc$set$floatingTickCount(NetHandlerPlayServer h, int value) {
        var listener = h.mcc$listener();
        var f = mcc$floatingField();
        try {
            if (listener != null && f != null) f.setInt(listener, value);
        } catch (IllegalAccessException e) {
            // left as is
        }
    }
}
