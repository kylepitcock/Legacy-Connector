package legacyconnector.v1_6_4.facade.net.minecraft.client;

import java.io.File;

/** Facade of the 1.7.10 shim's {@code Minecraft} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Minecraft {
    private Minecraft() {}

    /**
     * {@code Minecraft.getAppDir(name)}: the per-user application folder {@code .name} (1.4.7 and 1.5.2 kept saves and configs of
     * {@code minecraft} there). The game directory is the folder of {@code minecraft}; any other name is a sibling folder of it, created on demand.
     */
    public static File getAppDir(String name) {
        File game = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.getMinecraft().mcDataDir;
        if (name == null || name.equals("minecraft") || name.equals(".minecraft")) return game;
        File dir = new File(game.getAbsoluteFile().getParentFile(), name.startsWith(".") ? name : "." + name);
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) throw new RuntimeException("The working directory could not be created: " + dir);
        return dir;
    }
}
