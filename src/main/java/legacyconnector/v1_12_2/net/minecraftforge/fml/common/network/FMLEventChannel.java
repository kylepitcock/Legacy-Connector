package legacyconnector.v1_12_2.net.minecraftforge.fml.common.network;

/** An event-driven channel: mods register listeners for raw packets; the layer carries no raw packets, so listeners are only kept. */
public class FMLEventChannel {
    private final java.util.List<Object> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void register(Object handler) {
        listeners.add(handler);
    }

    public void unregister(Object handler) {
        listeners.remove(handler);
    }
}
