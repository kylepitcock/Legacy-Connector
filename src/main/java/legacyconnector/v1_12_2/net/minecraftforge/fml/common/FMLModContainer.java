package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/** Forge's container of an {@code @Mod} class; mods cast the active container to it. */
public abstract class FMLModContainer implements ModContainer {
    /** Mods (Pulsar) register their own listeners on a container's lifecycle bus. */
    private final com.google.common.eventbus.EventBus eventBus = new com.google.common.eventbus.EventBus();

    /**
     * FML's table of lifecycle handler methods by event class; mods (InfinityLib) read it by reflection and add their own handlers, which the
     * platform then calls next to the {@code @Mod.EventHandler} methods.
     */
    private final com.google.common.collect.ListMultimap<Class<?>, java.lang.reflect.Method> eventMethods = com.google.common.collect.ArrayListMultimap.create();

    public com.google.common.collect.ListMultimap<Class<?>, java.lang.reflect.Method> mcc$eventMethods() {
        return eventMethods;
    }
}
