package legacyconnector.v1_7_10.net.minecraft.world;

/**
 * A listener for world changes (block updates, sounds, particles, entities), registered with {@code World.addWorldAccess}. The modern
 * world reports these to its renderer and sound system itself, so legacy listeners are accepted but not called.
 */
public interface IWorldAccess {
    /** Static entities (paintings, item frames) changed; {@code World.func_147450_X} tells every listener. */
    default void onStaticEntitiesChanged() {
    }
}
