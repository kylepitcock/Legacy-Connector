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

    /**
     * 1.5.2 {@code installResource(path, file)} (a file of the resources folder or a mod's sound): "sound/...", "newsound/...", "streaming/..." and "music/..." files
     * register as sounds of that name; other kinds (the original's "title" images) are ignored.
     */
    public static void installResource(legacyconnector.v1_7_10.net.minecraft.client.Minecraft self, String path, File file) {
        if (path == null) return;
        int slash = path.indexOf('/');
        if (slash < 0) return;
        String kind = path.substring(0, slash);
        String name = path.substring(slash + 1);
        if (kind.equalsIgnoreCase("sound") || kind.equalsIgnoreCase("newsound") || kind.equalsIgnoreCase("streaming") || kind.equalsIgnoreCase("music")) {
            legacyconnector.v1_6_4.net.minecraft.client.audio.SoundPool.SOUNDS.addSound(name, file);
        }
    }

    /** 1.4.7 {@code isDebugInfoEnabled()}: whether the F3 overlay is on. */
    public static boolean isDebugInfoEnabled(legacyconnector.v1_7_10.net.minecraft.client.Minecraft self) {
        var settings = legacyconnector.v1_7_10.net.minecraft.client.Minecraft.mcc$get$gameSettings(self);
        return settings != null && settings.showDebugInfo;
    }
}
