package legacyconnector.v1_20_1.net.minecraftforge.registries;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.20.1's {@code ObjectHolderRegistry}: libraries register a handler that runs after object holders were refreshed (to refresh their own fields). The
 * layer runs the handlers where it injects its own holders ({@code ObjectHolders1201}), with a filter that accepts every name.
 */
public final class ObjectHolderRegistry {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Forge1201");
    private static final List<Consumer<Predicate<ResourceLocation>>> HANDLERS = new CopyOnWriteArrayList<>();

    private ObjectHolderRegistry() {}

    public static void addHandler(Consumer<Predicate<ResourceLocation>> handler) {
        HANDLERS.add(handler);
    }

    /** Runs every handler once for all names. A handler that throws is logged and skipped. */
    public static void applyObjectHolders() {
        for (Consumer<Predicate<ResourceLocation>> h : HANDLERS) {
            try {
                h.accept(name -> true);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("An object holder handler failed", e);
            }
        }
    }
}
