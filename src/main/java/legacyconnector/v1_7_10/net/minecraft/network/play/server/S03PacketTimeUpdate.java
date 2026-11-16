package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** World age and time of day; a negative time of day stops the sun (the daylight cycle is off). */
public class S03PacketTimeUpdate extends Packet {
    private long totalWorldTime;
    private long worldTime;

    public S03PacketTimeUpdate() {
    }

    public S03PacketTimeUpdate(long totalWorldTime, long worldTime, boolean doDaylightCycle) {
        this.totalWorldTime = totalWorldTime;
        this.worldTime = worldTime;
        if (!doDaylightCycle) {
            this.worldTime = -worldTime;
            if (this.worldTime == 0L) this.worldTime = -1L;
        }
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        totalWorldTime = data.readLong();
        worldTime = data.readLong();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeLong(totalWorldTime);
        data.writeLong(worldTime);
    }

    public long func_149366_c() {
        return totalWorldTime;
    }

    public long func_149365_d() {
        return worldTime;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(totalWorldTime, Math.abs(worldTime), worldTime >= 0));
    }
}
