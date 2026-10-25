package legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher;

import java.io.File;
import java.util.ArrayList;

/** What the Forge launcher handed to coremods: the version numbers, the game directory and the coremod list. */
public class FMLInjectionData {
    public static final String major = "14";
    public static final String minor = "23";
    public static final String rev = "5";
    public static final String build = "2860";
    public static final String mccversion = "1.12.2";
    public static final String mcpversion = "9.42";

    public static File minecraftHome() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile();
    }

    public static Object[] data() {
        return new Object[] {major, minor, rev, build, mccversion, mcpversion, minecraftHome(), new ArrayList<String>()};
    }

    public static String debfuscationDataName() {
        return "deobfuscation_data-" + mccversion + ".lzma";
    }
}
