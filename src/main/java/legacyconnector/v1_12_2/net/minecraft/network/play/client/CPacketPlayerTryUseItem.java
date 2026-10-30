package legacyconnector.v1_12_2.net.minecraft.network.play.client;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayServer;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;

/** The client uses the item in a hand (right click in the air); sent as the game's use-item packet with the player's current look. */
public class CPacketPlayerTryUseItem implements Packet<INetHandlerPlayServer> {
    private EnumHand hand;

    public CPacketPlayerTryUseItem() {
    }

    public CPacketPlayerTryUseItem(EnumHand handIn) {
        this.hand = handIn;
    }

    public EnumHand getHand() {
        return hand;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        hand = buf.readVarInt() == 1 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(hand == EnumHand.OFF_HAND ? 1 : 0);
    }

    @Override
    public void processPacket(INetHandlerPlayServer handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        float[] look = CPacketPlayerTryUseItemOnBlock.mcc$look();
        return new net.minecraft.network.protocol.game.ServerboundUseItemPacket(hand == null ? net.minecraft.world.InteractionHand.MAIN_HAND : hand.mcc$modern(), 0,
                look[0], look[1]);
    }
}
