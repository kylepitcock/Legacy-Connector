package legacyconnector.v1_12_2.net.minecraftforge.classloading;

/**
 * Forge's coremod plugin. {@link #RUNTIME_DEOBF} said whether the game's names were SRG names (production) or MCP names (a dev
 * environment). A released mod runs in production, so it is true: mods that only log or fail hard in a dev environment (GregTech's
 * invalid-recipe check) behave as they do for players, and mods that pick SRG names for reflection get them resolved by
 * {@code SrgNames} and the shim's {@code ReflectionHelper}, which accept both name sets.
 */
public class FMLForgePlugin {
    public static boolean RUNTIME_DEOBF = true;
}
