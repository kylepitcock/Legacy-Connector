package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.ByteBuffer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.kylepitcock.legacyconnector.core.network.LegacyNetwork;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;

/**
 * Player motion is client-driven, so the server never holds a player's real velocity. When an original server callback edits some
 * velocity components of a player (a bouncing block sets only vertical speed), only those components are sent and the client keeps its
 * own value for the rest; an absolute motion packet would otherwise replace the player's momentum with the server's stale copy.
 */
public final class PlayerMotionB173 {
    // its own family: receivers are keyed by family, and "b1.7.3" belongs to the ModLoader packets (PacketsB173)
    static final String FAMILY = "b1.7.3-motion";
    static final String CHANNEL = "player_motion";
    private PlayerMotionB173() {}

    public static void init() {
        LegacyNetwork.registerReceiver(FAMILY, PlayerMotionB173::receive);
    }

    static int mask(double vx, double vy, double vz, double bx, double by, double bz) {
        return (vx != bx ? 1 : 0) | (vy != by ? 2 : 0) | (vz != bz ? 4 : 0);
    }

    static Vec3 merge(Vec3 current, int mask, double vx, double vy, double vz) {
        return new Vec3((mask & 1) != 0 ? vx : current.x, (mask & 2) != 0 ? vy : current.y, (mask & 4) != 0 ? vz : current.z);
    }

    static byte[] encode(int mask, double vx, double vy, double vz) {
        return ByteBuffer.allocate(25).put((byte) mask).putDouble(vx).putDouble(vy).putDouble(vz).array();
    }
    /** Bit 8 of the mask: the values are changes the client adds to its own current velocity. */
    static final int DELTA = 8;

    /** The client's current velocity with the changed components moved by what original code changed them by. */
    static Vec3 add(Vec3 current, int mask, double dx, double dy, double dz) {
        return new Vec3((mask & 1) != 0 ? current.x + dx : current.x, (mask & 2) != 0 ? current.y + dy : current.y, (mask & 4) != 0 ? current.z + dz : current.z);
    }

    /**
     * The changes original code made to a server player's velocity, measured from the velocity its client last reported (the view's
     * baseline); the client adds them to its own. A component set to a value arrives as that value (baseline plus difference); one original
     * code added to (a jetpack's thrust) keeps the player's own momentum instead of replacing it with the server's copy.
     */
    public static boolean sendChange(ServerPlayer player, int mask, double vx, double vy, double vz, double bx, double by, double bz) {
        return send(player, mask | DELTA, vx - bx, vy - by, vz - bz);
    }

    private static final ThreadLocal<int[]> CLIENT_DRIVEN=ThreadLocal.withInitial(()->new int[1]);
    /** While a block's walking or collision callback runs for a server player whose client runs the same callback for itself. */
    public static AutoCloseable0 clientDriven(net.minecraft.world.entity.Entity entity){
        boolean own=entity instanceof ServerPlayer player&&player.connection!=null&&hasLegacyChannel(player);
        int[] depth=CLIENT_DRIVEN.get();if(own)depth[0]++;
        return ()->{if(own)depth[0]--;};
    }
    /** A server player whose motion from the running block callback its own client applies: the server sends none. */
    public static boolean clientDrives(){return CLIENT_DRIVEN.get()[0]>0;}
    public interface AutoCloseable0 extends AutoCloseable{@Override void close();}

    /** True when the changed components were delivered to the player's client, which applies them to its own velocity. */
    public static boolean send(ServerPlayer player, int mask, double vx, double vy, double vz) {
        if ((mask & 7) == 0 || player.connection == null || !hasLegacyChannel(player)) return false;
        // not kept on the server: its copy of a client-moved player's velocity is stale, and a push repeated every tick (a conveyor's) must
        // differ from it again next tick to be sent again
        PacketDistributor.sendToPlayer(player, new LegacyPayload(FAMILY, CHANNEL, 0, encode(mask, vx, vy, vz)));
        return true;
    }

    /** A fake or not yet connected player (tests, bots) has a connection object without a channel: no client to tell. */
    private static boolean hasLegacyChannel(ServerPlayer player) {
        try {
            return player.connection.hasChannel(LegacyPayload.TYPE);
        } catch (NullPointerException noChannel) {
            return false;
        }
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        if (!CHANNEL.equals(payload.channel()) || !context.flow().isClientbound() || payload.data().length != 25) return;
        var data = ByteBuffer.wrap(payload.data());
        int mask = data.get() & 15;
        double vx = data.getDouble(), vy = data.getDouble(), vz = data.getDouble();
        if (!Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) return;
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) player.setDeltaMovement((mask & DELTA) != 0 ? add(player.getDeltaMovement(), mask, vx, vy, vz)
                    : merge(player.getDeltaMovement(), mask, vx, vy, vz));
        });
    }
}
