package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;

/** Game state change: weather, game mode, credits, arrow hit sound. */
public class S2BPacketChangeGameState extends Packet {
    public static final String[] field_149142_a = {"tile.bed.notValid", null, null, "gameMode.changed"};

    private final int state;
    private final float value;

    public S2BPacketChangeGameState(int state, float value) {
        this.state = state;
        this.value = value;
    }

    public int func_149138_c() {
        return state;
    }

    public float func_149137_d() {
        return value;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        ClientboundGameEventPacket.Type type = switch (state) {
            case 0 -> ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE;
            case 1 -> ClientboundGameEventPacket.START_RAINING;
            case 2 -> ClientboundGameEventPacket.STOP_RAINING;
            case 3 -> ClientboundGameEventPacket.CHANGE_GAME_MODE;
            case 4 -> ClientboundGameEventPacket.WIN_GAME;
            case 5 -> ClientboundGameEventPacket.DEMO_EVENT;
            case 6 -> ClientboundGameEventPacket.ARROW_HIT_PLAYER;
            case 7 -> ClientboundGameEventPacket.RAIN_LEVEL_CHANGE;
            case 8 -> ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE;
            default -> null;
        };
        if (type != null) player.connection.send(new ClientboundGameEventPacket(type, value));
    }
}
