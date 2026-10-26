package legacyconnector.v1_7_10.net.minecraftforge.event.world;

import java.util.List;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.world.Explosion;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Posted before an explosion ({@link Start}, cancelable) and once it knows what it hits ({@link Detonate}). */
public class ExplosionEvent extends Event {
    public final World world;
    public final Explosion explosion;

    public ExplosionEvent(World world, Explosion explosion) {
        this.world = world;
        this.explosion = explosion;
    }

    @Cancelable
    public static class Start extends ExplosionEvent {
        public Start(World world, Explosion explosion) {
            super(world, explosion);
        }
    }

    /** Listeners may remove entries from both lists to spare those blocks ({@code ChunkPosition}s) and entities. */
    public static class Detonate extends ExplosionEvent {
        private final List<Entity> entityList;

        public Detonate(World world, Explosion explosion, List<Entity> entityList) {
            super(world, explosion);
            this.entityList = entityList;
        }

        public List getAffectedBlocks() {
            return explosion.affectedBlockPositions;
        }

        public List<Entity> getAffectedEntities() {
            return entityList;
        }
    }
}
