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
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickItem",
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$LeftClickBlock");

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
        return isKnownSourceCancelableBuiltin(event);
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
        if (!isCancelable(event)) {
            throw notCancelable(event);
        }
        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(canceled);
            return;
        }
        throw new IllegalStateException(
                "Cancelable source Forge event has no target ICancellableEvent implementation: "
                        + event.getClass().getName());
    }

    /** Non-virtual equivalent of Forge Event.isCancelable(). */
    public static boolean baseIsCancelable(Event event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof Forge1201CancelableEventMarker) {
            return true;
        }

        return isKnownSourceCancelableBuiltin(event);
    }

    /**
     * Exact fallback for target built-ins that replace source Forge event classes.
     * ICancellableEvent alone is deliberately insufficient: target interface
     * membership is not proof that the corresponding Forge 1.20.1 class had
     * @Cancelable.
     */
    private static boolean isKnownSourceCancelableBuiltin(Event event) {
        Class<?> type = event.getClass();
        while (type != null && Event.class.isAssignableFrom(type)) {
            if (BUILTIN_SOURCE_CANCELABLE_TARGET_TYPES.contains(type.getName().replace('.', '/'))) {
                return true;
            }
            type = type.getSuperclass();
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
