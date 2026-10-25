package org.kylepitcock.legacyconnector.core.event;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version-neutral implementation of the legacy Forge event bus semantics: annotation-driven subscription,
 * dispatch to listeners of the event class and all its superclasses, priority ordering and cancellation.
 * Each shim's {@code EventBus} class is a thin facade over one of these.
 */
public final class ReflectiveEventBus {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/EventBus");
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger> FAILURES =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** (handler, event class) pairs already reported as cancelling: which mod hides something is the first thing to know. */
    private static final java.util.Set<String> CANCELLERS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Version-specific knowledge: which annotation marks handlers and how events report cancellation. */
    public interface Adapter {
        Class<? extends Annotation> subscribeAnnotation();

        Class<?> eventBaseClass();

        /** Higher runs first. */
        int priority(Annotation subscribe);

        boolean receiveCanceled(Annotation subscribe);

        boolean isCanceled(Object event);
    }

    private record Listener(Object owner, Class<?> eventType, int priority, boolean receiveCanceled, MethodHandle handle, String name, Class<?> generic) {}

    /** The type argument of a handler's event parameter ({@code Register<Item>} gives Item), or null for raw and wildcard parameters. */
    private static Class<?> genericOf(Method m) {
        try {
            if (m.getGenericParameterTypes()[0] instanceof java.lang.reflect.ParameterizedType p && p.getActualTypeArguments().length == 1) {
                java.lang.reflect.Type t = p.getActualTypeArguments()[0];
                if (t instanceof Class<?> c) return c;
                if (t instanceof java.lang.reflect.ParameterizedType pt && pt.getRawType() instanceof Class<?> c) return c;
            }
        } catch (RuntimeException | LinkageError e) {
            // a synthesized event class has no type parameters, so its generic signature does not parse
        }
        return null;
    }

    private final String name;
    private final Adapter adapter;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Object, List<Listener>> byOwner = new IdentityHashMap<>();
    private final Map<Class<?>, List<Listener>> dispatchCache = new ConcurrentHashMap<>();
    /** Event classes that have at least one listener (including via superclass); lets bridges skip work. */
    private final Map<Class<?>, Boolean> interest = new ConcurrentHashMap<>();

    public ReflectiveEventBus(String name, Adapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    /**
     * Registers instance handlers of {@code target}, or static handlers if {@code target} is a {@link Class}
     * (the 1.12 {@code @EventBusSubscriber} style).
     */
    public synchronized void register(Object target) {
        if (byOwner.containsKey(target)) return;
        boolean statics = target instanceof Class<?>;
        Class<?> type = statics ? (Class<?>) target : target.getClass();
        List<Listener> added = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Annotation sub = m.getAnnotation(adapter.subscribeAnnotation());
                if (sub == null) continue;
                if (Modifier.isStatic(m.getModifiers()) != statics) continue;
                if (m.getParameterCount() != 1 || !adapter.eventBaseClass().isAssignableFrom(m.getParameterTypes()[0])) {
                    LOG.warn("[{}] Ignoring @SubscribeEvent method {} with invalid signature", name, m);
                    continue;
                }
                if (added.stream().anyMatch(l -> l.name.equals(m.getName()) && l.eventType == m.getParameterTypes()[0])) continue; // overridden
                try {
                    m.setAccessible(true);
                    MethodHandle h = MethodHandles.lookup().unreflect(m);
                    if (!statics) h = h.bindTo(target);
                    added.add(new Listener(target, m.getParameterTypes()[0], adapter.priority(sub), adapter.receiveCanceled(sub), h,
                            m.getDeclaringClass().getName() + "." + m.getName(), genericOf(m)));
                } catch (IllegalAccessException | RuntimeException e) {
                    LOG.error("[{}] Could not access event handler {}", name, m, e);
                }
            }
        }
        listeners.addAll(added);
        byOwner.put(target, added);
        dispatchCache.clear();
        interest.clear();
    }

    public synchronized void unregister(Object target) {
        List<Listener> removed = byOwner.remove(target);
        if (removed != null) {
            listeners.removeAll(removed);
            dispatchCache.clear();
            interest.clear();
        }
    }

    /** @return true if the event was canceled */
    public boolean post(Object event) {
        List<Listener> targets = dispatchCache.computeIfAbsent(event.getClass(), this::collect);
        for (Listener l : targets) {
            boolean canceledBefore = adapter.isCanceled(event);
            if (!l.receiveCanceled && canceledBefore) continue;
            if (l.generic != null && event instanceof GenericEventType g && g.mcc$genericType() != l.generic) continue;
            try {
                l.handle.invoke(event);
                if (!canceledBefore && adapter.isCanceled(event) && CANCELLERS.add(l.name + " " + event.getClass().getName())) {
                    LOG.info("[{}] Legacy handler {} cancels {} (reported once)", name, l.name, event.getClass().getName());
                }
            } catch (Throwable t) {
                // A handler that fails every tick would flood the log (and the disk) in seconds: say so a few times.
                int n = FAILURES.computeIfAbsent(l.name, k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
                if (n <= 3) {
                    LOG.error("[{}] Exception in legacy event handler {} for {}", name, l.name, event.getClass().getName(), t);
                } else if (n == 4 || n % 5000 == 0) {
                    LOG.error("[{}] Legacy event handler {} keeps failing ({} times so far, last: {}); further failures are not logged", name,
                            l.name, n, t.toString());
                }
                if (t instanceof Error err && !(t instanceof LinkageError)) throw err;
            }
        }
        return adapter.isCanceled(event);
    }

    public boolean hasListeners(Class<?> eventType) {
        return interest.computeIfAbsent(eventType, t -> !collect(t).isEmpty());
    }

    private List<Listener> collect(Class<?> eventType) {
        List<Listener> out = new ArrayList<>();
        for (Listener l : listeners) {
            if (l.eventType.isAssignableFrom(eventType)) out.add(l);
        }
        out.sort(Comparator.comparingInt((Listener l) -> -l.priority));
        return out;
    }

    @Override
    public String toString() {
        return "LegacyEventBus[" + name + ", " + listeners.size() + " listeners]";
    }
}
