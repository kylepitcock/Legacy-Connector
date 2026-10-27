package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** Particles the client spawns around a point. */
public class SPacketParticles implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes particleType;
    private boolean longDistance;
    private float xCoord;
    private float yCoord;
    private float zCoord;
    private float xOffset;
    private float yOffset;
    private float zOffset;
    private float particleSpeed;
    private int particleCount;
    private int[] particleArguments;

    public SPacketParticles() {
    }

    public SPacketParticles(legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes particleIn, boolean longDistanceIn, float xIn, float yIn, float zIn,
            float xOffsetIn, float yOffsetIn, float zOffsetIn, float speedIn, int countIn, int... argumentsIn) {
        this.particleType = particleIn;
        this.longDistance = longDistanceIn;
        this.xCoord = xIn;
        this.yCoord = yIn;
        this.zCoord = zIn;
        this.xOffset = xOffsetIn;
        this.yOffset = yOffsetIn;
        this.zOffset = zOffsetIn;
        this.particleSpeed = speedIn;
        this.particleCount = countIn;
        this.particleArguments = argumentsIn;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
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
        // a count of 0 meant "one particle, the offsets are its speed" (and the colour for coloured kinds)
        var options = particleType == null ? null : particleType.mcc$modern(xOffset, yOffset, zOffset, particleArguments == null ? new int[0] : particleArguments);
        if (options == null) return null;
        return new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(options, longDistance, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset,
                particleSpeed, particleCount);
    }
}
