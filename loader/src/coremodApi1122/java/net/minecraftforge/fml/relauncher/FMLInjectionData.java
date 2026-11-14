package net.minecraftforge.fml.relauncher;

import java.io.File;
import net.minecraft.launchwrapper.Launch;

public class FMLInjectionData {
    /** The Minecraft version of the legacy mods being loaded; the loader sets it. */
    public static String mcVersion = "1.12.2";
    public static File minecraftHome;
    public static final java.util.List<String> containers = new java.util.ArrayList<>();

    public static Object[] data() {
        return new Object[] {"14", "23", "5", "2860", mcVersion, "9.42", Launch.minecraftHome, containers};
    }

    public static File debugOutputLocation() {
        return Launch.minecraftHome;
    }
}
