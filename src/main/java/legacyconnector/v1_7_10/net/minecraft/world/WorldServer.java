package legacyconnector.v1_7_10.net.minecraft.world;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/** Server-side world view. {@link World#mcc$wrap} returns instances of this class for server levels. */
public class WorldServer extends World {
    protected WorldServer(LevelAccessor level) {
        super(level);
    }

    public legacyconnector.v1_7_10.net.minecraft.server.management.PlayerManager getPlayerManager() {
        return mcc$fullLevel() instanceof ServerLevel s ? new legacyconnector.v1_7_10.net.minecraft.server.management.PlayerManager(s) : null;
    }

    public MinecraftServer func_73046_m() {
        return mcc$fullLevel() instanceof ServerLevel s ? MinecraftServer.mcc$wrap(s.getServer()) : null;
    }

    public java.io.File getChunkSaveLocation() {
        return getSaveHandler().getWorldDirectory();
    }

    public boolean areAllPlayersAsleep() {
        return mcc$fullLevel() instanceof ServerLevel s && s.players().stream().allMatch(net.minecraft.world.entity.player.Player::isSleeping);
    }

    public void resetUpdateEntityTick() {
    }

    public void removePlayerEntityDangerously(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
    }
}
