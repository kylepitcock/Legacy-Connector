package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's named sound at a position (coordinates in 1/8 block, pitch in 1/63). */
public class Packet62LevelSound extends Packet {
    private String soundName;
    private int effectX = Integer.MAX_VALUE;
    private int effectY;
    private int effectZ;
    private float volume;
    private int pitch;

    public Packet62LevelSound() {}

    public Packet62LevelSound(String name, double x, double y, double z, float volume, float pitch) {
        this.soundName = name;
        this.effectX = (int) (x * 8.0D);
        this.effectY = (int) (y * 8.0D);
        this.effectZ = (int) (z * 8.0D);
        this.volume = volume;
        this.pitch = Math.max(0, Math.min(255, (int) (pitch * 63.0F)));
    }

    public String getSoundName() {
        return soundName;
    }

    public double getEffectX() {
        return effectX / 8.0F;
    }

    public double getEffectY() {
        return effectY / 8.0F;
    }

    public double getEffectZ() {
        return effectZ / 8.0F;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch / 63.0F;
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        soundName = in.readUTF();
        effectX = in.readInt();
        effectY = in.readInt();
        effectZ = in.readInt();
        volume = in.readFloat();
        pitch = in.readUnsignedByte();
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeUTF(soundName);
        out.writeInt(effectX);
        out.writeInt(effectY);
        out.writeInt(effectZ);
        out.writeFloat(volume);
        out.writeByte(pitch);
    }

    @Override
    public int getPacketSize() {
        return 24;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        new S29PacketSoundEffect(soundName, getEffectX(), getEffectY(), getEffectZ(), volume, getPitch()).mcc$send(player);
    }
}
