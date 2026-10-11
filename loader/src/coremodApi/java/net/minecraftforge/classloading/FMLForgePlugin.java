package net.minecraftforge.classloading;

import java.io.File;

/** Forge's own coremod: other coremods read whether names are obfuscated (SRG at runtime in production). */
public class FMLForgePlugin {
    public static boolean RUNTIME_DEOBF = true;
    public static File forgeLocation;
}
