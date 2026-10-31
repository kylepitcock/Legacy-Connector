package legacyconnector.v1_7_10.net.minecraftforge.classloading;

import java.io.File;

/**
 * Forge's own coremod. {@link #RUNTIME_DEOBF} said whether the game ran on SRG names: true in every released 1.7.10
 * client and server, false only in a development workspace. Mods take their developer code paths when it is false:
 * DragonAPI's {@code ReikaObfuscationHelper.isDeObfEnvironment()} is {@code !RUNTIME_DEOBF}, and ChromatiCraft's data
 * node then looks up the player "Reika" every tick and throws (a NullPointerException posted to chat each tick). So it
 * is true, as in production; the SRG names such mods then reflect with are renamed by the loader when they are literals
 * and mapped by {@code ReflectionCompat}/{@code SrgNames} otherwise. Matches the coremod-side copy and
 * {@code Launch.blackboard["fml.deobfuscatedEnvironment"] == false}.
 */
public class FMLForgePlugin {
    public static boolean RUNTIME_DEOBF = true;
    public static File forgeLocation;
}
