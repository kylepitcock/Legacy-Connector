package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

/** Which side the game runs on. */
public class FMLLaunchHandler {
    public static Side side() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? Side.CLIENT : Side.SERVER;
    }

    public static boolean isDeobfuscatedEnvironment() {
        return true;
    }
}
