package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A sound played by its resource name (a sound that is no registered event, e.g. from a resource pack) at a position. */
public class SPacketCustomSound implements Packet<INetHandlerPlayClient> {
    private String soundName = "";
    private legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category = legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.MASTER;
    private int x;
    private int y = Integer.MAX_VALUE;
    private int z;
    private float volume;
    private float pitch;

    public SPacketCustomSound() {
    }

    public SPacketCustomSound(String soundName, legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, double x, double y, double z,
            float volume, float pitch) {
        this.soundName = soundName;
        this.category = category;
        this.x = (int) (x * 8.0D);
        this.y = (int) (y * 8.0D);
        this.z = (int) (z * 8.0D);
        this.volume = volume;
        this.pitch = pitch;
    }

    public String getSoundName() {
        return soundName;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundCategory getCategory() {
        return category;
    }

    public double getX() {
        return x / 8.0F;
    }

    public double getY() {
        return y / 8.0F;
    }

    public double getZ() {
        return z / 8.0F;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        soundName = buf.readString(256);
        category = buf.readEnumValue(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.class);
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeString(soundName);
        buf.writeEnumValue(category);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        var id = net.minecraft.resources.ResourceLocation.tryParse(soundName);
        if (id == null) return null;
        net.minecraft.sounds.SoundSource source;
        try {
            source = net.minecraft.sounds.SoundSource.valueOf(category.name());
        } catch (RuntimeException e) {
            source = net.minecraft.sounds.SoundSource.MASTER;
        }
        return new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)), source, x / 8.0D, y / 8.0D, z / 8.0D,
                volume, pitch, java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }
}
