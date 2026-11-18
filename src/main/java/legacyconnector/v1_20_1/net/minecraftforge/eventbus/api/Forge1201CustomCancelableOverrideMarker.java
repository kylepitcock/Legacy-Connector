package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

/**
 * Records that the adapter added ICancellableEvent because a source event
 * overrides isCancelable(), rather than because the source class hierarchy
 * carried @Cancelable. The marker lets a rewritten Forge super.isCancelable()
 * ignore this synthetic target-interface addition.
 */
public interface Forge1201CustomCancelableOverrideMarker {
}
