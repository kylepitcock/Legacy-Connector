package mccompat.v1_7_10.net.minecraft.client.multiplayer;

import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.level.LevelAccessor;

/** Client world view; {@code World.mcc$wrap} returns this for client levels. */
public class WorldClient extends World {
    public WorldClient(LevelAccessor level) {
        super(level);
    }
}
