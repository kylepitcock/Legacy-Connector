package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.world.EnumDifficulty;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSettings;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;

/**
 * Tells the client to reload into a dimension. Legacy mods send it after moving a player between dimensions by hand; the
 * modern teleport already switches the client's level, so the packet carries data only.
 */
public class S07PacketRespawn extends Packet {
    private final int dimension;
    private final EnumDifficulty difficulty;
    private final WorldType worldType;
    private final WorldSettings.GameType gameType;

    public S07PacketRespawn(int dimension, EnumDifficulty difficulty, WorldType worldType, WorldSettings.GameType gameType) {
        this.dimension = dimension;
        this.difficulty = difficulty;
        this.worldType = worldType;
        this.gameType = gameType;
    }

    public int func_149082_c() {
        return dimension;
    }

    public EnumDifficulty func_149081_d() {
        return difficulty;
    }

    public WorldType func_149080_f() {
        return worldType;
    }

    public WorldSettings.GameType func_149083_e() {
        return gameType;
    }
}
