package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** Experience bar progress, total points and level. */
public class S1FPacketSetExperience extends Packet {
    private final float progress;
    private final int total;
    private final int level;

    public S1FPacketSetExperience(float progress, int total, int level) {
        this.progress = progress;
        this.total = total;
        this.level = level;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(progress, total, level));
    }
}
