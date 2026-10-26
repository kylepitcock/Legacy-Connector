package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;

/** A potion effect ended on an entity. */
public class S1EPacketRemoveEntityEffect extends Packet {
    public int field_149079_a;
    public int field_149078_b;

    public S1EPacketRemoveEntityEffect() {
    }

    public S1EPacketRemoveEntityEffect(int entityId, legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect effect) {
        this.field_149079_a = entityId;
        this.field_149078_b = effect.getPotionID();
        this.mcc$effect = effect;
    }

    private legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect mcc$effect;

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149079_a = data.readInt();
        field_149078_b = data.readUnsignedByte();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeInt(field_149079_a);
        data.writeByte(field_149078_b);
    }

    public int func_149076_c() {
        return field_149079_a;
    }

    public int func_149075_d() {
        return field_149078_b;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var effect = mcc$effect != null ? mcc$effect : new legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect(field_149078_b, 0);
        var inst = effect.mcc$modern();
        if (inst != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(field_149079_a, inst.getEffect()));
    }
}
