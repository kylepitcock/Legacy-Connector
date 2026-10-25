package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

/** Forge's container of an {@code @Mod} class; mods cast the active container to it. */
public abstract class FMLModContainer implements ModContainer {
    /** Mods (Pulsar) register their own listeners on a container's lifecycle bus. */
    private final com.google.common.eventbus.EventBus eventBus = new com.google.common.eventbus.EventBus();
}
