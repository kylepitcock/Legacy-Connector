package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A named sound at a position (coordinates in 1/8 block, pitch in 1/63). */
public class S29PacketSoundEffect extends Packet {
    public String field_149219_a;
    public int field_149217_b;
    public int field_149218_c = Integer.MAX_VALUE;
    public int field_149215_d;
    public float field_149216_e;
    public int field_149214_f;

    public S29PacketSoundEffect() {
    }

    public S29PacketSoundEffect(String sound, double x, double y, double z, float volume, float pitch) {
        this.field_149219_a = sound;
        this.field_149217_b = (int) (x * 8.0D);
        this.field_149218_c = (int) (y * 8.0D);
        this.field_149215_d = (int) (z * 8.0D);
        this.field_149216_e = volume;
        this.field_149214_f = (int) (pitch * 63.0F);
        if (this.field_149214_f < 0) this.field_149214_f = 0;
        if (this.field_149214_f > 255) this.field_149214_f = 255;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149219_a = data.readStringFromBuffer(256);
        field_149217_b = data.readInt();
        field_149218_c = data.readInt();
        field_149215_d = data.readInt();
        field_149216_e = data.readFloat();
        field_149214_f = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(field_149219_a);
        data.writeInt(field_149217_b);
        data.writeInt(field_149218_c);
        data.writeInt(field_149215_d);
        data.writeFloat(field_149216_e);
        data.writeByte(field_149214_f);
    }

    public String func_149212_c() {
        return field_149219_a;
    }

    public double func_149207_d() {
        return field_149217_b / 8.0F;
    }

    public double func_149211_e() {
        return field_149218_c / 8.0F;
    }

    public double func_149210_f() {
        return field_149215_d / 8.0F;
    }

    public float func_149208_g() {
        return field_149216_e;
    }

    public float func_149209_h() {
        return field_149214_f / 63.0F;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var sound = org.kylepitcock.legacyconnector.v1_7_10.bridge.SoundBridge.sound(field_149219_a);
        if (sound == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(sound),
                net.minecraft.sounds.SoundSource.BLOCKS, func_149207_d(), func_149211_e(), func_149210_f(), field_149216_e, func_149209_h(),
                player.getRandom().nextLong()));
    }
}
