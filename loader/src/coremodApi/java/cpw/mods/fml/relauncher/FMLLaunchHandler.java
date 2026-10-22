package cpw.mods.fml.relauncher;

public class FMLLaunchHandler {
    public static Side side() {
        return "SERVER".equals(System.getProperty("legacyconnector.side")) ? Side.SERVER : Side.CLIENT;
    }
}
