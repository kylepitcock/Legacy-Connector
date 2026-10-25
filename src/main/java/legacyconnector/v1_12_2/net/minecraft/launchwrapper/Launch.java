package legacyconnector.v1_12_2.net.minecraft.launchwrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;

/**
 * LaunchWrapper's entry point, as far as mods observe it: the shared blackboard, the class loader and the game
 * directory. Nothing is launched through it; modlauncher already did that.
 */
public class Launch {
    public static File minecraftHome = FMLPaths.GAMEDIR.get().toFile();
    public static File assetsDir = new File(minecraftHome, "assets");
    public static Map<String, Object> blackboard = new HashMap<>();
    public static LaunchClassLoader classLoader = new LaunchClassLoader(Launch.class.getClassLoader());

    static {
        // Translated mods run deobfuscated-by-name (MCP names), but must take their "production" code paths.
        blackboard.put("fml.deobfuscatedEnvironment", Boolean.FALSE);
        blackboard.put("Tweaks", new ArrayList<>());
        blackboard.put("TweakClasses", new ArrayList<String>());
        blackboard.put("ArgumentList", new ArrayList<String>());
    }
}
