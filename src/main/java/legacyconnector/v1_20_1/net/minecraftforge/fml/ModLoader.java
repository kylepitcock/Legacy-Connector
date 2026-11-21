package legacyconnector.v1_20_1.net.minecraftforge.fml;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Forge 1.20.1's {@code ModLoader}: a singleton ({@code ModLoader.get()}) with instance methods. NeoForge 1.21.1's class has static methods only, and its
 * events must be mod bus events; an event that is not one is not posted (nothing listens to it on a mod bus).
 */
public final class ModLoader {
    private static final ModLoader INSTANCE = new ModLoader();

    private ModLoader() {
    }

    public static ModLoader get() {
        return INSTANCE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> void postEvent(T event) {
        if (event instanceof IModBusEvent) net.neoforged.fml.ModLoader.postEvent((Event & IModBusEvent) event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> void postEventWrapContainerInModOrder(T event) {
        if (event instanceof IModBusEvent) net.neoforged.fml.ModLoader.postEventWrapContainerInModOrder((Event & IModBusEvent) event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> T postEventWithReturn(T event) {
        if (event instanceof IModBusEvent) net.neoforged.fml.ModLoader.postEvent((Event & IModBusEvent) event);
        return event;
    }

    /**
     * {@code ModLoader.get().runEventGenerator(container -> event)}: one event per mod, built from that mod's container and sent to that mod's own bus
     * (Cracker's Wither Storm and CrackersLib gather per-mod registrations this way). An event that is not a mod bus event is not posted.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> void runEventGenerator(java.util.function.Function<net.neoforged.fml.ModContainer, T> generator) {
        for (net.neoforged.fml.ModContainer container : net.neoforged.fml.ModList.get().getSortedMods()) {
            T event = generator.apply(container);
            if (event instanceof IModBusEvent) container.acceptEvent((Event & IModBusEvent) event);
        }
    }

    /** Whether no loading error has happened so far. */
    public static boolean isLoadingStateValid() {
        return !net.neoforged.fml.ModLoader.hasErrors();
    }

    /** {@code ModLoader.get().addWarning(warning)}: listed on the loading warnings screen. */
    public void addWarning(ModLoadingWarning warning) {
        net.neoforged.fml.ModLoader.addLoadingIssue(warning.toIssue());
    }

    public boolean hasErrors() {
        return net.neoforged.fml.ModLoader.hasErrors();
    }
}
