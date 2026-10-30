package legacyconnector.v1_12_2.net.minecraftforge.classloading;

/**
 * Forge's coremod plugin. {@link #RUNTIME_DEOBF} said whether the game's names were SRG names (production) or MCP names (a dev
 * environment). The shim's classes carry MCP names, so it is false, matching {@code FMLLaunchHandler.isDeobfuscatedEnvironment()}:
 * mods then reflect with the MCP names the shim declares.
 */
public class FMLForgePlugin {
    public static boolean RUNTIME_DEOBF = false;
}
