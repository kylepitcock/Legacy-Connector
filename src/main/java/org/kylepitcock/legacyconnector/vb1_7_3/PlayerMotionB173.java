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
    static final String FAMILY = "b1.7.3";
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

    /** True when the changed components were delivered to the player's client, which applies them to its own velocity. */
    public static boolean send(ServerPlayer player, int mask, double vx, double vy, double vz) {
        if (mask == 0 || player.connection == null || !player.connection.hasChannel(LegacyPayload.TYPE)) return false;
        player.setDeltaMovement(merge(player.getDeltaMovement(), mask, vx, vy, vz));
        PacketDistributor.sendToPlayer(player, new LegacyPayload(FAMILY, CHANNEL, 0, encode(mask, vx, vy, vz)));
        return true;
    }

    private static void receive(LegacyPayload payload, IPayloadContext context) {
        if (!CHANNEL.equals(payload.channel()) || !context.flow().isClientbound() || payload.data().length != 25) return;
        var data = ByteBuffer.wrap(payload.data());
        int mask = data.get() & 7;
        double vx = data.getDouble(), vy = data.getDouble(), vz = data.getDouble();
        if (!Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) return;
        context.enqueueWork(() -> {
            var player = context.player();
            if (player != null) player.setDeltaMovement(merge(player.getDeltaMovement(), mask, vx, vy, vz));
        });
    }
}
