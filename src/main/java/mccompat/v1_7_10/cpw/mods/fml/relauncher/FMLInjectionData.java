package mccompat.v1_7_10.cpw.mods.fml.relauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;

/** FML's launch data: the FML/Minecraft/MCP versions of the 1.7.10 environment legacy mods expect. */
public class FMLInjectionData {
    static File minecraftHome = FMLPaths.GAMEDIR.get().toFile();
    static String major = "7";
    static String minor = "10";
    static String rev = "99";
    static String build = "1614";
    static String mccversion = "1.7.10";
    static String mcpversion = "9.05";
    public static final List<String> containers = new ArrayList<>();

    public static Object[] data() {
        return new Object[] {major, minor, rev, build, mccversion, mcpversion, minecraftHome, containers};
    }
}
