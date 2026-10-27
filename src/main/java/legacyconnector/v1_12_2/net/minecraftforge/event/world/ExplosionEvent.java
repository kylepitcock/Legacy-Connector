package legacyconnector.v1_12_2.net.minecraftforge.event.world;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.Explosion;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Forge's explosion events, posted from the modern ones (Start may cancel; Detonate may change what is destroyed and hit). */
public class ExplosionEvent extends Event {
    private final World world;
    private final Explosion explosion;

    public ExplosionEvent(World world, Explosion explosion) {
        this.world = world;
        this.explosion = explosion;
    }

    public World getWorld() {
        return world;
    }

    public Explosion getExplosion() {
        return explosion;
    }

    @Cancelable
    public static class Start extends ExplosionEvent {
        public Start(World world, Explosion explosion) {
            super(world, explosion);
        }
    }

    public static class Detonate extends ExplosionEvent {
        private final List<Entity> entityList;

        public Detonate(World world, Explosion explosion, List<Entity> entityList) {
            super(world, explosion);
            this.entityList = entityList;
        }

        public List<BlockPos> getAffectedBlocks() {
            return getExplosion().getAffectedBlockPositions();
        }

        public List<Entity> getAffectedEntities() {
            return entityList;
        }
    }
}
