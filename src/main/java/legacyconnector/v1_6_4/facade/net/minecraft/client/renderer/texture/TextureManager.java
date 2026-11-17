package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.texture;

/** Facade of the 1.7.10 shim's {@code TextureManager} for 1.4.7 and 1.5.2 mods (their {@code RenderEngine}). See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class TextureManager {
    private TextureManager() {}

    /** {@code deleteTexture(glId)}: frees the GL texture a mod made with {@code allocateAndSetupTexture} or {@code createTextureFromBytes}. */
    public static void deleteTexture(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager self, int glId) {
        if (glId > 0) com.mojang.blaze3d.platform.GlStateManager._deleteTexture(glId);
    }
}
