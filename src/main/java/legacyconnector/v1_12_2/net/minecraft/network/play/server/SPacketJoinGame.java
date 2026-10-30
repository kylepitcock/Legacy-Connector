package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty;
import legacyconnector.v1_12_2.net.minecraft.world.GameType;
import legacyconnector.v1_12_2.net.minecraft.world.WorldType;

/**
 * The login packet: the player's entity id, game mode and world. The modern server sends its own at login (a second one would reset
 * the client), so this one is data for mods that read it and is never sent.
 */
public class SPacketJoinGame implements Packet<INetHandlerPlayClient> {
    private int playerId;
    private boolean hardcoreMode;
    private GameType gameType;
    private int dimension;
    private EnumDifficulty difficulty;
    private int maxPlayers;
    private WorldType worldType;
    private boolean reducedDebugInfo;

    public SPacketJoinGame() {
    }

    public SPacketJoinGame(int playerIdIn, GameType gameTypeIn, boolean hardcoreModeIn, int dimensionIn, EnumDifficulty difficultyIn,
            int maxPlayersIn, WorldType worldTypeIn, boolean reducedDebugInfoIn) {
        this.playerId = playerIdIn;
        this.dimension = dimensionIn;
        this.difficulty = difficultyIn;
        this.gameType = gameTypeIn;
        this.maxPlayers = maxPlayersIn;
        this.hardcoreMode = hardcoreModeIn;
        this.worldType = worldTypeIn;
        this.reducedDebugInfo = reducedDebugInfoIn;
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isHardcoreMode() {
        return hardcoreMode;
    }

    public GameType getGameType() {
        return gameType;
    }

    public int getDimension() {
        return dimension;
    }

    public EnumDifficulty getDifficulty() {
        return difficulty;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public WorldType getWorldType() {
        return worldType;
    }

    public boolean isReducedDebugInfo() {
        return reducedDebugInfo;
    }

    @Override
    public void readPacketData(PacketBuffer buf) throws IOException {
        // the modern client reads the modern login packet
    }

    @Override
    public void writePacketData(PacketBuffer buf) throws IOException {
        // never sent (see the class comment)
    }

    @Override
    public void processPacket(INetHandlerPlayClient handler) {
    }
}
