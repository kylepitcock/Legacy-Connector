package legacyconnector.v1_6_4.facade.net.minecraft.network;

import java.lang.reflect.Field;
import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Manager164;

/**
 * Facade of the 1.7.10 shim's {@code NetHandlerPlayServer} for 1.6.4 mods ({@code NetServerHandler}): {@code netManager} typed as the 1.6.4
 * {@link INetworkManager}, the floating-kick counter and the packet size. See {@code Hierarchy.facadeOf}. Never instantiated.
 */
public final class NetHandlerPlayServer {
    private NetHandlerPlayServer() {}

    private static volatile Field floatCounter;
    private static volatile boolean floatCounterMissing;

    /** NetServerHandler.netManager */
    public static INetworkManager mcc$get$netManager(legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer self) {
        return Manager164.of(self);
    }

    /** NetServerHandler.packetSize(): bytes queued for the connection (nothing here is queued by the layer). */
    public static int packetSize(legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer self) {
        return 0;
    }

    /** NetServerHandler.ticksForFloatKick: ticks the player has been in the air (the server kicks for flying past a limit). */
    public static int mcc$get$ticksForFloatKick(legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer self) {
        Field f = counter();
        var listener = listener(self);
        if (f == null || listener == null) return 0;
        try {
            return f.getInt(listener);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    /** Mods (flight and jetpack mods) set it to 0 every tick so the server does not kick their users for floating. */
    public static void mcc$set$ticksForFloatKick(legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer self, int value) {
        Field f = counter();
        var listener = listener(self);
        if (f == null || listener == null) return;
        try {
            f.setInt(listener, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // keep going: the kick check is the only thing that reads it
        }
    }

    private static net.minecraft.server.network.ServerGamePacketListenerImpl listener(legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer self) {
        var player = self.playerEntity == null ? null : self.playerEntity.mcc$serverPlayer();
        return player == null ? null : player.connection;
    }

    private static Field counter() {
        if (floatCounter != null || floatCounterMissing) return floatCounter;
        synchronized (NetHandlerPlayServer.class) {
            if (floatCounter == null && !floatCounterMissing) {
                try {
                    Field f = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                    f.setAccessible(true);
                    floatCounter = f;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    floatCounterMissing = true;
                }
            }
        }
        return floatCounter;
    }
}
