package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

/**
 * Marker applied to classes that directly carried Forge 1.20.1's
 * {@code @Cancelable} annotation. JVM interface inheritance preserves Forge's
 * superclass-inherited cancelability semantics for their descendants.
 *
 * <p>This marker is intentionally separate from NeoForge's
 * {@code ICancellableEvent}: it records source annotation provenance only.</p>
 */
public interface Forge1201CancelableEventMarker {
}
