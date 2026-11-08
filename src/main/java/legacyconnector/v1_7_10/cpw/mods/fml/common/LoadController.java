package legacyconnector.v1_7_10.cpw.mods.fml.common;

/** FML's mod load controller; coremod containers receive one when they register their event bus. */
public class LoadController {
    public LoadController() {
    }

    /** A mod reports an error while loading: logged against the mod (the layer decides itself whether the pack still starts). */
    public void errorOccurred(ModContainer mod, Throwable error) {
        String id = mod == null ? "unknown" : mod.getModId();
        org.apache.logging.log4j.LogManager.getLogger("LegacyConnector").error("Mod {} reported a loading error", id, error);
    }
}
