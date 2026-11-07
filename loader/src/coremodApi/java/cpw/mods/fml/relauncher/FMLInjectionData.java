package cpw.mods.fml.relauncher;

import java.io.File;
import net.minecraft.launchwrapper.Launch;

public class FMLInjectionData {
    /** The Minecraft version of the legacy mods being loaded; the loader sets it ("1.6.4" or "1.5.2" for the numeric-id versions). */
    public static String mcVersion = "1.7.10";

    public static Object[] data() {
        return new Object[] {"7", "10", "99", "1614", mcVersion, "9.05", Launch.minecraftHome, java.util.List.of()};
    }
}
