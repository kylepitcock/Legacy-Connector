package legacyconnector.v1_6_4.facade.cpw.mods.fml.common;

/** Facade of the 1.7.10 shim's {@code FMLCommonHandler} for 1.6.4 mods: {@code getFMLLogger()} (a {@code java.util.logging.Logger}). */
public final class FMLCommonHandler {
    private FMLCommonHandler() {}

    /** 1.6's {@code getBrandings()}: the lines of the title screen's version block (1.7.10 takes whether to include Minecraft's own line). */
    public static java.util.List<String> getBrandings(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler self) {
        return self.getBrandings(true);
    }

    public static java.util.logging.Logger getFMLLogger(legacyconnector.v1_7_10.cpw.mods.fml.common.FMLCommonHandler self) {
        return FMLLog.getLogger();
    }
}
