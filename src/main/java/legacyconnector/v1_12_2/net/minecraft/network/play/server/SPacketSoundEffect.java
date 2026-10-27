package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A sound at a position. */
public class SPacketSoundEffect implements Packet<INetHandlerPlayClient> {
    private legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound;
    private legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category;
    private double posX;
    private double posY;
    private double posZ;
    private float soundVolume;
    private float soundPitch;

    public SPacketSoundEffect() {
    }

    public SPacketSoundEffect(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent soundIn, legacyconnector.v1_12_2.net.minecraft.util.SoundCategory categoryIn,
            double xIn, double yIn, double zIn, float volumeIn, float pitchIn) {
        this.sound = soundIn;
        this.category = categoryIn;
        this.posX = xIn;
        this.posY = yIn;
        this.posZ = zIn;
        this.soundVolume = volumeIn;
        this.soundPitch = pitchIn;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundEvent getSound() {
        return sound;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.SoundCategory getCategory() {
        return category;
    }

    public double getX() {
        return posX;
    }

    public double getY() {
        return posY;
    }

    public double getZ() {
        return posZ;
    }

    public float getVolume() {
        return soundVolume;
    }

    public float getPitch() {
        return soundPitch;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        sound = legacyconnector.v1_12_2.net.minecraft.util.SoundEvent.REGISTRY.getObjectById(buf.readVarInt());
        category = buf.readEnumValue(legacyconnector.v1_12_2.net.minecraft.util.SoundCategory.class);
        posX = buf.readInt() / 8.0F;
        posY = buf.readInt() / 8.0F;
        posZ = buf.readInt() / 8.0F;
        soundVolume = buf.readFloat();
        soundPitch = buf.readFloat();
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(legacyconnector.v1_12_2.net.minecraft.util.SoundEvent.REGISTRY.getIDForObject(sound));
        buf.writeEnumValue(category);
        buf.writeInt((int) (posX * 8.0D));
        buf.writeInt((int) (posY * 8.0D));
        buf.writeInt((int) (posZ * 8.0D));
        buf.writeFloat(soundVolume);
        buf.writeFloat(soundPitch);
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        net.minecraft.sounds.SoundEvent modern = sound == null ? null : sound.mcc$modern;
        if (modern == null && sound != null && sound.getRegistryName() != null) {
            modern = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(sound.getRegistryName().mcc$modern());
        }
        if (modern == null) return null;
        net.minecraft.sounds.SoundSource source;
        try {
            source = net.minecraft.sounds.SoundSource.valueOf(category.name());
        } catch (RuntimeException e) {
            source = net.minecraft.sounds.SoundSource.MASTER;
        }
        return new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(modern), source, posX, posY, posZ,
                soundVolume, soundPitch, java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }
}
