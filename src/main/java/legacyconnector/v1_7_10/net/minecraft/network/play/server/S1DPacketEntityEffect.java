package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect;

/** Shows a potion effect on an entity (the effect itself is applied by the entity). */
public class S1DPacketEntityEffect extends Packet {
    private final int entityId;
    private final PotionEffect effect;

    public S1DPacketEntityEffect(int entityId, PotionEffect effect) {
        this.entityId = entityId;
        this.effect = effect;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        var inst = effect.mcc$modern();
        if (inst != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(entityId, inst, false));
    }
}
