package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** The world time (a negative day time stops the daylight cycle). */
public class SPacketTimeUpdate implements Packet<INetHandlerPlayClient> {
    private long totalWorldTime;
    private long worldTime;
    private boolean doDaylightCycle;

    public SPacketTimeUpdate() {
    }

    public SPacketTimeUpdate(long totalWorldTimeIn, long worldTimeIn, boolean doDaylightCycleIn) {
        this.totalWorldTime = totalWorldTimeIn;
        this.worldTime = worldTimeIn;
        this.doDaylightCycle = doDaylightCycleIn;
    }

    public long mcc$totalWorldTime() {
        return totalWorldTime;
    }

    public long mcc$worldTime() {
        return worldTime;
    }

    public boolean mcc$doDaylightCycle() {
        return doDaylightCycle;
    }


    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ClientboundSetTimePacket(totalWorldTime, worldTime, doDaylightCycle);
    }
}
