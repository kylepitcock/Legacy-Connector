package legacyconnector.v1_7_10.net.minecraft.server.management;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** The per-player block breaking/interaction manager (1.7.10); modern code keeps this in the game mode. */
public class ItemInWorldManager {
    public World theWorld;
    private final net.minecraft.server.level.ServerPlayer mcc$player;

    public ItemInWorldManager(World world) {
        this.theWorld = world;
        this.mcc$player = null;
    }

    public ItemInWorldManager(World world, net.minecraft.server.level.ServerPlayer player) {
        this.theWorld = world;
        this.mcc$player = player;
    }

    public double getBlockReachDistance() {
        return mcc$player != null ? mcc$player.blockInteractionRange() : 5.0D;
    }

    public void setBlockReachDistance(double distance) {
    }

    public boolean isCreative() {
        return mcc$player != null && mcc$player.isCreative();
    }

    public boolean tryHarvestBlock(int x, int y, int z) {
        if (mcc$player == null) return false;
        return mcc$player.gameMode.destroyBlock(new net.minecraft.core.BlockPos(x, y, z));
    }

    public legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType getGameType() {
        if (mcc$player == null) return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.SURVIVAL;
        return legacyconnector.v1_7_10.net.minecraft.world.WorldSettings.GameType.getByID(mcc$player.gameMode.getGameModeForPlayer().getId());
    }

    public void setWorld(legacyconnector.v1_7_10.net.minecraft.world.WorldServer world) {
        this.theWorld = world;
    }
}
