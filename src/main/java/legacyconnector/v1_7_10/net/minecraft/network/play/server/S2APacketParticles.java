package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** Particles at a position: a legacy particle name, the spread on each axis, a speed and a count. */
public class S2APacketParticles extends Packet {
    private String particleName = "";
    private float xCoord;
    private float yCoord;
    private float zCoord;
    private float xOffset;
    private float yOffset;
    private float zOffset;
    private float particleSpeed;
    private int particleCount;

    public S2APacketParticles() {
    }

    public S2APacketParticles(String name, float x, float y, float z, float xOffset, float yOffset, float zOffset, float speed, int count) {
        this.particleName = name;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.particleSpeed = speed;
        this.particleCount = count;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        particleName = data.readStringFromBuffer(64);
        xCoord = data.readFloat();
        yCoord = data.readFloat();
        zCoord = data.readFloat();
        xOffset = data.readFloat();
        yOffset = data.readFloat();
        zOffset = data.readFloat();
        particleSpeed = data.readFloat();
        particleCount = data.readInt();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(particleName);
        data.writeFloat(xCoord);
        data.writeFloat(yCoord);
        data.writeFloat(zCoord);
        data.writeFloat(xOffset);
        data.writeFloat(yOffset);
        data.writeFloat(zOffset);
        data.writeFloat(particleSpeed);
        data.writeInt(particleCount);
    }

    public String func_149228_c() {
        return particleName;
    }

    public double func_149220_d() {
        return xCoord;
    }

    public double func_149226_e() {
        return yCoord;
    }

    public double func_149225_f() {
        return zCoord;
    }

    public float func_149221_g() {
        return xOffset;
    }

    public float func_149224_h() {
        return yOffset;
    }

    public float func_149223_i() {
        return zOffset;
    }

    public float func_149227_j() {
        return particleSpeed;
    }

    public int func_149222_k() {
        return particleCount;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var options = org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge.particle(particleName, xOffset, yOffset, zOffset);
        if (options == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(options, false, xCoord, yCoord, zCoord,
                xOffset, yOffset, zOffset, particleSpeed, particleCount));
    }
}
