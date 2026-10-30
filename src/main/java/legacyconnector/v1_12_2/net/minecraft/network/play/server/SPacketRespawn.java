package legacyconnector.v1_12_2.net.minecraft.network.play.server;

import java.io.IOException;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_12_2.net.minecraft.network.play.INetHandlerPlayClient;
import legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty;
import legacyconnector.v1_12_2.net.minecraft.world.GameType;
import legacyconnector.v1_12_2.net.minecraft.world.WorldType;

/**
 * Makes the client set up a (new) world: the dimension, difficulty, world type and game mode. Sent as the modern respawn packet for
 * that dimension's level (mods send it when they move a player between dimensions themselves).
 */
public class SPacketRespawn implements Packet<INetHandlerPlayClient> {
    private int dimensionID;
    private EnumDifficulty difficulty;
    private GameType gameType;
    private WorldType worldType;

    public SPacketRespawn() {
    }

    public SPacketRespawn(int dimensionIdIn, EnumDifficulty difficultyIn, WorldType worldTypeIn, GameType gameModeIn) {
        this.dimensionID = dimensionIdIn;
        this.difficulty = difficultyIn;
        this.gameType = gameModeIn;
        this.worldType = worldTypeIn;
    }

    public int getDimensionID() {
        return dimensionID;
    }

    public EnumDifficulty getDifficulty() {
        return difficulty;
    }

    public GameType getGameType() {
        return gameType;
    }

    public WorldType getWorldType() {
        return worldType;
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
        var level = legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager.mcc$level(dimensionID);
        if (level == null) return null;
        var mode = gameType == null || gameType.mcc$modern() == null ? net.minecraft.world.level.GameType.SURVIVAL : gameType.mcc$modern();
        var info = new net.minecraft.network.protocol.game.CommonPlayerSpawnInfo(level.dimensionTypeRegistration(), level.dimension(),
                net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(level.getSeed()), mode, null, level.isDebug(), level.isFlat(),
                java.util.Optional.empty(), 0);
        return new net.minecraft.network.protocol.game.ClientboundRespawnPacket(info, net.minecraft.network.protocol.game.ClientboundRespawnPacket.KEEP_ALL_DATA);
    }
}
