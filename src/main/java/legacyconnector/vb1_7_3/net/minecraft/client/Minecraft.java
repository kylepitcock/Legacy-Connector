package legacyconnector.vb1_7_3.net.minecraft.client;

import java.io.File;

/**
 * Beta 1.7.3 {@code Minecraft}: only its static directory helpers so far (mods put their config files under
 * {@code getMinecraftDir()}). There is no legacy client instance yet: {@code ModLoader.getMinecraftInstance()} still answers null.
 */
public class Minecraft {
    protected Minecraft() {}

    /** MCP {@code getMinecraftDir()}: the game directory. */
    public static File getRunDirectory() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile();
    }

    /** MCP {@code getAppDir(String)}: also the game directory (b1.7.3 used ~/.minecraft for "minecraft"). */
    public static File getApplicationDirectory(String name) {
        return getRunDirectory();
    }
}
