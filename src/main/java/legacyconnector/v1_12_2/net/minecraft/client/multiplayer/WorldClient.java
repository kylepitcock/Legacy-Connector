package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The client's world ({@code Minecraft.world}): a view over the modern client level. */
public class WorldClient extends World {
    public WorldClient(net.minecraft.world.level.Level level) {
        super(level);
    }
}
