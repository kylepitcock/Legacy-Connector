package legacyconnector.v1_6_4.net.minecraft.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.play.server.S1DPacketEntityEffect;
import legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect;
import net.minecraft.server.level.ServerPlayer;

/** 1.6.4's entity effect packet: a potion effect an entity got (id, amplifier, duration in ticks). */
public class Packet41EntityEffect extends Packet {
    public int entityId;
    public byte effectId;
    public byte effectAmplifier;
    public short duration;
    private PotionEffect effect;

    public Packet41EntityEffect() {}

    public Packet41EntityEffect(int entityId, PotionEffect effect) {
        this.entityId = entityId;
        this.effect = effect;
        this.effectId = (byte) effect.getPotionID();
        this.effectAmplifier = (byte) effect.getAmplifier();
        this.duration = (short) Math.min(effect.getDuration(), Short.MAX_VALUE);
    }

    @Override
    public void readPacketData(DataInput in) throws IOException {
        entityId = in.readInt();
        effectId = in.readByte();
        effectAmplifier = in.readByte();
        duration = in.readShort();
        effect = new PotionEffect(effectId, duration, effectAmplifier);
    }

    @Override
    public void writePacketData(DataOutput out) throws IOException {
        out.writeInt(entityId);
        out.writeByte(effectId);
        out.writeByte(effectAmplifier);
        out.writeShort(duration);
    }

    @Override
    public int getPacketSize() {
        return 8;
    }

    @Override
    public void mcc$send(ServerPlayer player) {
        if (effect != null) new S1DPacketEntityEffect(entityId, effect).mcc$send(player);
    }
}
