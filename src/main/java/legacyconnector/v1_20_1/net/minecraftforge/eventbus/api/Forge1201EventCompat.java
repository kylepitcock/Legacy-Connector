package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

/**
 * Runtime compatibility surface for transformed Forge 1.20.1 Event subclasses.
 *
 * <p>The NeoForge 1.21.1 base Event no longer declares Forge's
 * {@code isCancelable()}, {@code isCanceled()} or {@code setCanceled(boolean)}
 * methods. Transformed legacy event roots implement this interface so call sites
 * whose symbolic owner was the old Forge Event can preserve virtual dispatch.</p>
 *
 * <p>This interface deliberately does not extend {@code ICancellableEvent}.
 * Plain Forge events must remain plain target events so NeoForge Bus can unwrap
 * cancellation-check listeners for them.</p>
 */
public interface Forge1201EventCompat {
    boolean isCancelable();

    boolean isCanceled();

    void setCanceled(boolean canceled);
}
