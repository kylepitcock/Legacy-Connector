package legacyconnector.vb1_7_3.net.minecraft.client;

import java.io.File;

import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/**
 * Beta 1.7.3 {@code Minecraft}: the static directory helpers and one instance ({@code ModLoader.getMinecraftInstance()}). A ModLoader mod
 * ran on one world, the client's; here its code runs on the server's and the client's level, so {@link #world} is the legacy view of the
 * level of the callback being run (IndustrialCraft 2 decides by {@code mc.theWorld.isRemote} whether it simulates). GUIs find the texture
 * manager and the font here and open screens with {@link #setScreen}; the player, renderer and settings fields are synthesized for now.
 */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();

    public World world;
    public int displayWidth = 854;
    public int displayHeight = 480;
    public volatile boolean paused;
    public TextureManager textureManager = TextureManager.mcc$instance();
    public TextRenderer textRenderer = TextRenderer.mcc$instance();
    /** The legacy screen shown, if any. */
    public Screen currentScreen;

    protected Minecraft() {}

    public static Minecraft mcc$instance() {
        return INSTANCE;
    }

    /** The world legacy code is about to run on (set before a block callback or a mod's tick). */
    public static void mcc$use(World world) {
        if (world != null) INSTANCE.world = world;
    }

    /** MCP {@code displayGuiScreen}: shows a legacy screen on the client (null closes it). */
    public void setScreen(Screen screen) {
        MenusB173.setScreen(screen);
    }

    /** MCP {@code getMinecraftDir()}: the game directory. */
    public static File getRunDirectory() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile();
    }

    /** MCP {@code getAppDir(String)}: also the game directory (b1.7.3 used ~/.minecraft for "minecraft"). */
    public static File getApplicationDirectory(String name) {
        return getRunDirectory();
    }
}
