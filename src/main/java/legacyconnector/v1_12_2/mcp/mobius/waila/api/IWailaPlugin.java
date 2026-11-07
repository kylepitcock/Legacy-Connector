package legacyconnector.v1_12_2.mcp.mobius.waila.api;

/** The entry point of a Waila integration (found by its {@link WailaPlugin} annotation). */
public interface IWailaPlugin {
    void register(IWailaRegistrar registrar);
}
