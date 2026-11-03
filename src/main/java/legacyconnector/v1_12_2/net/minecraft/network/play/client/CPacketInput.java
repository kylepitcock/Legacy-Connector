package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;

/** The movement input a riding player sends (strafe, forward, jump, sneak). */
public class CPacketInput implements Packet<INetHandlerPlayServer> {
    private float strafeSpeed;
    private float forwardSpeed;
    private boolean jumping;
    private boolean sneaking;

    public CPacketInput() {
    }

    public CPacketInput(float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking) {
        this.strafeSpeed = strafeSpeed;
        this.forwardSpeed = forwardSpeed;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        strafeSpeed = buf.readFloat();
        forwardSpeed = buf.readFloat();
        byte flags = buf.readByte();
        jumping = (flags & 1) > 0;
        sneaking = (flags & 2) > 0;
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeFloat(strafeSpeed);
        buf.writeFloat(forwardSpeed);
        byte flags = 0;
        if (jumping) flags = (byte) (flags | 1);
        if (sneaking) flags = (byte) (flags | 2);
        buf.writeByte(flags);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    public float getStrafeSpeed() {
        return strafeSpeed;
    }

    public float getForwardSpeed() {
        return forwardSpeed;
    }

    public boolean isJumping() {
        return jumping;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        return new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(strafeSpeed, forwardSpeed, jumping, sneaking);
    }
}
