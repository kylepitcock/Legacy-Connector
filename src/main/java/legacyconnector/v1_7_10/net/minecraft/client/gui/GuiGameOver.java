package legacyconnector.v1_7_10.net.minecraft.client.gui;

/**
 * 1.7.10's death screen ("You died!", respawn and title screen buttons). Displaying one opens the modern death screen (see ClientGui), which
 * respawns through the modern connection; mods open it to show the screen themselves (cinematic respawn, hardcore changes) or recognise it.
 */
public class GuiGameOver extends GuiScreen {
    public GuiGameOver() {
    }
}
