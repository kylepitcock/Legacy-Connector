package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;

/** A game state change: 1 start rain, 2 stop rain, 3 game mode, 4 credits, 5 demo, 6 arrow hit, 7 rain level, 8 thunder level, 9 puffer fish, 10 elder guardian. */
public class SPacketChangeGameState implements Packet<INetHandlerPlayClient> {
    private int state;
    private float value;

    public SPacketChangeGameState() {
    }

    public SPacketChangeGameState(int stateIn, float valueIn) {
        this.state = stateIn;
        this.value = valueIn;
    }

    public int mcc$state() {
        return state;
    }

    public float mcc$value() {
        return value;
    }

    public int getGameState() {
        return state;
    }

    public float getValue() {
        return value;
    }


    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // read by the modern client from the modern packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // written as the modern packet
    }

    /** Handled by the modern client as the modern packet. */
    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }

    @Override
    public net.minecraft.network.protocol.Packet<?> mcc$modern() {
        net.minecraft.network.protocol.game.ClientboundGameEventPacket.Type t = switch (state) {
            case 0 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE;
            case 1 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.START_RAINING;
            case 2 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.STOP_RAINING;
            case 3 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.CHANGE_GAME_MODE;
            case 4 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.WIN_GAME;
            case 5 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.DEMO_EVENT;
            case 6 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.ARROW_HIT_PLAYER;
            case 7 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE;
            case 8 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE;
            case 9 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.PUFFER_FISH_STING;
            case 10 -> net.minecraft.network.protocol.game.ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT;
            default -> null;
        };
        return t == null ? null : new net.minecraft.network.protocol.game.ClientboundGameEventPacket(t, value);
    }
}
