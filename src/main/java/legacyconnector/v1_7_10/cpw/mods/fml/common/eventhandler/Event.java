package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class of all legacy events. Legacy FML injected {@code isCancelable()/hasResult()} overrides with an ASM
 * transformer based on the {@link Cancelable}/{@link HasResult} annotations; here they are computed from the
 * annotations directly (cached per class).
 */
public class Event {
    private static final Map<Class<?>, Boolean> CANCELABLE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> HAS_RESULT = new ConcurrentHashMap<>();

    private boolean isCanceled;
    private Result result = Result.DEFAULT;
    private EventPriority phase;

    public Event() {
    }

    public boolean isCancelable() {
        return CANCELABLE.computeIfAbsent(getClass(), c -> annotated(c, Cancelable.class));
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean cancel) {
        if (!isCancelable()) {
            throw new IllegalArgumentException("Attempted to cancel a uncancelable event");
        }
        isCanceled = cancel;
    }

    public boolean hasResult() {
        return HAS_RESULT.computeIfAbsent(getClass(), c -> annotated(c, HasResult.class));
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result value) {
        result = value;
    }

    public EventPriority getPhase() {
        return phase;
    }

    public void setPhase(EventPriority value) {
        phase = value;
    }

    private static boolean annotated(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
        for (; c != null && c != Object.class; c = c.getSuperclass()) {
            if (c.isAnnotationPresent(a)) return true;
        }
        return false;
    }

    public enum Result {
        DENY, DEFAULT, ALLOW
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface HasResult {
    }
}
