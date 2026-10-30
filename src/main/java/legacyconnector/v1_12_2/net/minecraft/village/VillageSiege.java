package legacyconnector.v1_12_2.net.minecraft.village;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * A zombie siege of a village at midnight (the modern siege spawner: a village of at least ten beds and twenty villagers near a player,
 * one siege a night). The overworld runs its own; this one runs where a mod ticks it.
 */
public class VillageSiege {
    private final World world;
    private final net.minecraft.world.entity.ai.village.VillageSiege mcc$modern = new net.minecraft.world.entity.ai.village.VillageSiege();

    public VillageSiege(World world) {
        this.world = world;
    }

    public void tick() {
        if (world != null && world.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            mcc$modern.tick(level, true, false);
        }
    }
}
