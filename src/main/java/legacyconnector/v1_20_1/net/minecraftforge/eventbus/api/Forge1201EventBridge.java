package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Set;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Implements the Forge 1.20.1 Event cancellation contract on NeoForge 1.21.1.
 *
 * <p>Forge's base Event methods were removed from the target Event class. This
 * bridge is used for bytecode call sites whose original symbolic owner was the
 * Forge base Event. The base* methods represent non-virtual {@code super}
 * behavior and must not dispatch through a subclass override.</p>
 */
public final class Forge1201EventBridge {
    private static final VarHandle CANCELED;

    /**
     * Pinned NeoForge 21.1.258 target classes corresponding to source Forge 1.20.1
     * built-in event classes directly annotated with @Cancelable. Do not replace this
     * with a blanket ICancellableEvent check: target cancellation membership can differ
     * from source annotation semantics for some migrated built-ins.
     */
    private static final Set<String> BUILTIN_SOURCE_CANCELABLE_TARGET_TYPES = Set.of(
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$EntityInteract",
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$EntityInteractSpecific",
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock",
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickItem");

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Event.class, MethodHandles.lookup());
            CANCELED = lookup.findVarHandle(Event.class, "isCanceled", boolean.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private Forge1201EventBridge() {
    }

    /** Equivalent to a normal virtual call to the source Forge Event method. */
    public static boolean isCancelable(Event event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof Forge1201EventCompat compatible) {
            return compatible.isCancelable();
        }
        return event instanceof ICancellableEvent;
    }

    /** Equivalent to a normal virtual call to the source Forge Event method. */
    public static boolean isCanceled(Event event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof Forge1201EventCompat compatible) {
            return compatible.isCanceled();
        }
        if (event instanceof ICancellableEvent cancellable) {
            return cancellable.isCanceled();
        }
        return (boolean) CANCELED.get(event);
    }

    /** Equivalent to a normal virtual call to the source Forge Event method. */
    public static void setCanceled(Event event, boolean canceled) {
        Objects.requireNonNull(event, "event");
        if (event instanceof Forge1201EventCompat compatible) {
            compatible.setCanceled(canceled);
            return;
        }
        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(canceled);
            return;
        }
        throw notCancelable(event);
    }

    /** Non-virtual equivalent of Forge Event.isCancelable(). */
    public static boolean baseIsCancelable(Event event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof Forge1201CancelableEventMarker) {
            return true;
        }

        // Transformed source custom event parents inherit the connector marker.
        // Known NeoForge built-in replacements are handled by an exact type table,
        // not by assuming every target ICancellableEvent was source @Cancelable.
        Class<?> superclass = event.getClass().getSuperclass();
        while (superclass != null && Event.class.isAssignableFrom(superclass)) {
            if (BUILTIN_SOURCE_CANCELABLE_TARGET_TYPES.contains(superclass.getName().replace('.', '/'))) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }
        return false;
    }

    /** Non-virtual equivalent of Forge Event.isCanceled(): read the base Event state. */
    public static boolean baseIsCanceled(Event event) {
        Objects.requireNonNull(event, "event");
        return (boolean) CANCELED.get(event);
    }

    /** Non-virtual equivalent of Forge Event.setCanceled(), including its virtual cancelability check. */
    public static void baseSetCanceled(Event event, boolean canceled) {
        Objects.requireNonNull(event, "event");
        if (!isCancelable(event)) {
            throw notCancelable(event);
        }
        CANCELED.set(event, canceled);
    }

    private static UnsupportedOperationException notCancelable(Event event) {
        return new UnsupportedOperationException(
                "Attempted to call Event#setCanceled() on a non-cancelable event of type: "
                        + event.getClass().getCanonicalName());
    }
}
