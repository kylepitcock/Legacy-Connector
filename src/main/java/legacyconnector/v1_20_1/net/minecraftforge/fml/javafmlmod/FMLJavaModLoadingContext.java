package legacyconnector.v1_20_1.net.minecraftforge.fml.javafmlmod;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;

/**
 * Forge 1.20.1's way for a mod constructor to reach its mod event bus ({@code FMLJavaModLoadingContext.get().getModEventBus()}). NeoForge 1.21.1 injects the
 * bus into the constructor instead; this reads it from the container FML is constructing the mod for.
 *
 * <p>Approximation: 1.21.1 runs {@code Bootstrap.validate} (vanilla registry bootstrap, which runs mixins of mods) before any mod container exists, so a mod
 * whose class initializer is reached from such a mixin (it asks for its bus there) finds no {@link ModList}. It gets a recording bus instead, keyed by the
 * module that asked; the recorded registrations are replayed into that mod's real bus at the first request after the containers exist (a mod constructor).
 */
public class FMLJavaModLoadingContext {
    private static final FMLJavaModLoadingContext INSTANCE = new FMLJavaModLoadingContext();
    /** Recording buses by owner (the module name of the caller, which is the mod id for translated mods); replayed and dropped once the containers exist. */
    private static final Map<String, Recorder> EARLY = new ConcurrentHashMap<>();

    private FMLJavaModLoadingContext() {
    }

    public static FMLJavaModLoadingContext get() {
        return INSTANCE;
    }

    private static final class Recorder {
        final List<Object[]> calls = new ArrayList<>();
    }

    public IEventBus getModEventBus() {
        if (ModList.get() == null) {
            Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
            String owner = caller.getModule().getName();
            if (owner == null) owner = caller.getName();
            Recorder rec = EARLY.computeIfAbsent(owner, k -> new Recorder());
            return (IEventBus) Proxy.newProxyInstance(IEventBus.class.getClassLoader(), new Class<?>[] {IEventBus.class}, (proxy, m, args) -> {
                String n = m.getName();
                if (n.equals("addListener") || n.equals("register") || n.equals("addGenericListener")) {
                    synchronized (rec.calls) {
                        rec.calls.add(new Object[] {m, args});
                    }
                    return null;
                }
                if (n.equals("post") && args != null && args.length > 0) return args[0];
                if (n.equals("hashCode")) return System.identityHashCode(proxy);
                if (n.equals("equals")) return proxy == args[0];
                if (n.equals("toString")) return "early mod bus";
                Class<?> r = m.getReturnType();
                return r == boolean.class ? (Object) Boolean.FALSE : r.isPrimitive() ? (Object) 0 : null;
            });
        }
        flushEarly();
        return ModLoadingContext.get().getActiveContainer().getEventBus();
    }

    public ModContainer getContainer() {
        flushEarly();
        return ModLoadingContext.get().getActiveContainer();
    }

    private static void flushEarly() {
        if (EARLY.isEmpty() || ModList.get() == null) return;
        for (String owner : new ArrayList<>(EARLY.keySet())) {
            var container = ModList.get().getModContainerById(owner);
            if (container.isEmpty()) continue;
            Recorder rec = EARLY.remove(owner);
            if (rec == null) continue;
            IEventBus bus = container.get().getEventBus();
            if (bus == null) continue;
            synchronized (rec.calls) {
                for (Object[] c : rec.calls) {
                    try {
                        ((Method) c[0]).invoke(bus, (Object[]) c[1]);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        org.slf4j.LoggerFactory.getLogger("legacyconnector").warn("Could not replay an early event bus registration of {}: {}", owner, e.toString());
                    }
                }
            }
        }
    }
}
