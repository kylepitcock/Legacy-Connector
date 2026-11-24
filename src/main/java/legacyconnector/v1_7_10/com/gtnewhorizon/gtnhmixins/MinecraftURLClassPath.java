package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins;

import java.io.File;

/** GTNH Mixins' helper that finds jars in the mods folder and adds them to the class path (the layer's loader owns the class path, so nothing is added). */
public final class MinecraftURLClassPath {
    private MinecraftURLClassPath() {}

    /** The jar of the mods folder (or a version folder of it) whose file name contains {@code name}, or null. */
    public static File getJarInModPath(String name) {
        if (name == null) return null;
        File[] dirs = {new File("mods"), new File("mods/1.7.10")};
        for (File dir : dirs) {
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File f : files) if (f.isFile() && f.getName().endsWith(".jar") && f.getName().contains(name)) return f;
        }
        return null;
    }

    public static boolean findJarInClassPath(String name) {
        return false;
    }

    public static void addJar(File jar) throws Exception {
        // the layer loads legacy jars through its own class loader
    }
}
