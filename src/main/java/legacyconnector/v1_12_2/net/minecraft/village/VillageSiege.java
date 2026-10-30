package legacyconnector.v1_12_2.net.minecraft.village;

/**
 * 1.12.2 zombie siege of a village at night ({@code WorldServer.villageSiege}). Each {@link #tick} runs the modern game's own
 * siege spawner (vanilla's successor, which keeps the same night/village rules) once more for the world.
 */
public class VillageSiege {
    private final legacyconnector.v1_12_2.net.minecraft.world.World world;
    private final net.minecraft.world.entity.ai.village.VillageSiege modern = new net.minecraft.world.entity.ai.village.VillageSiege();

    public VillageSiege(legacyconnector.v1_12_2.net.minecraft.world.World world) {
        this.world = world;
    }

    public void tick() {
        if (world == null || !(world.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        try {
            modern.tick(level, level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL, true);
        } catch (RuntimeException e) {
            // a siege that cannot start this tick
        }
    }
}
