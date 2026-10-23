package legacyconnector.v1_7_10.net.minecraftforge.client;

import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.WorldClient;

/** Forge's hook for a dimension to draw its own sky, clouds or weather (see WorldProvider#getSkyRenderer). */
public abstract class IRenderHandler {
    public abstract void render(float partialTicks, WorldClient world, Minecraft mc);
}
