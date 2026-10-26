package legacyconnector.v1_7_10.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** The movement keys of a player riding an entity. */
public class C0CPacketInput extends Packet {
    public float strafeSpeed;
    public float forwardSpeed;
    public boolean jumping;
    public boolean sneaking;

    public C0CPacketInput() {
    }

    public C0CPacketInput(float strafeSpeed, float forwardSpeed, boolean jumping, boolean sneaking) {
        this.strafeSpeed = strafeSpeed;
        this.forwardSpeed = forwardSpeed;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        strafeSpeed = data.readFloat();
        forwardSpeed = data.readFloat();
        jumping = data.readBoolean();
        sneaking = data.readBoolean();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeFloat(strafeSpeed);
        data.writeFloat(forwardSpeed);
        data.writeBoolean(jumping);
        data.writeBoolean(sneaking);
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
    public void mcc$sendToServer() {
        org.kylepitcock.legacyconnector.v1_7_10.client.ClientNetwork1710.send(
                new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(strafeSpeed, forwardSpeed, jumping, sneaking));
    }
}
