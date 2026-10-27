package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

/** Minimal stub (area R1: entity rendering) so {@code Minecraft.getRenderManager()} can return it; R1's class replaces it. */
public class RenderManager {
    public final legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager renderEngine;
    private final legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer;

    public RenderManager(legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureManager renderEngine,
            legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem itemRenderer) {
        this.renderEngine = renderEngine;
        this.itemRenderer = itemRenderer;
    }
}
