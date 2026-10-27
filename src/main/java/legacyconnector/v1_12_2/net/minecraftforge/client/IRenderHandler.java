package legacyconnector.v1_12_2.net.minecraftforge.client;

/**
 * A dimension's own sky, cloud or weather renderer ({@code WorldProvider.setSkyRenderer}). Calling it from the game's sky
 * rendering belongs to area W's dimension effects (not wired yet).
 */
public abstract class IRenderHandler {
    public IRenderHandler() {
    }

    public abstract void render(float partialTicks, legacyconnector.v1_12_2.net.minecraft.client.multiplayer.WorldClient world,
            legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc);
}
