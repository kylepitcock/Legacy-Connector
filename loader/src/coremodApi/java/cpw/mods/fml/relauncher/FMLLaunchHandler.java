package cpw.mods.fml.relauncher;

public class FMLLaunchHandler {
    public static Side side() {
        return "SERVER".equals(System.getProperty("mccompat.side")) ? Side.SERVER : Side.CLIENT;
    }
}
