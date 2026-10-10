package mccompat.v1_7_10.net.minecraft.world;

import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/** Server-side world view. {@link World#mcc$wrap} returns instances of this class for server levels. */
public class WorldServer extends World {
    protected WorldServer(LevelAccessor level) {
        super(level);
    }

    public MinecraftServer func_73046_m() {
        return mcc$fullLevel() instanceof ServerLevel s ? MinecraftServer.mcc$wrap(s.getServer()) : null;
    }

    public boolean areAllPlayersAsleep() {
        return mcc$fullLevel() instanceof ServerLevel s && s.players().stream().allMatch(net.minecraft.world.entity.player.Player::isSleeping);
    }
}
