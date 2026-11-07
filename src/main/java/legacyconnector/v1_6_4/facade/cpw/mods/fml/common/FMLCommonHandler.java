package legacyconnector.v1_6_4.facade.cpw.mods.fml.common;

/** Facade of the 1.7.10 shim's {@code FMLCommonHandler} for 1.6.4 mods: {@code getFMLLogger()} (a {@code java.util.logging.Logger}). */
public final class FMLCommonHandler {
    private FMLCommonHandler() {}

    public static java.util.logging.Logger getFMLLogger(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler self) {
        return FMLLog.getLogger();
    }
}
