package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * Restores AlienEvo's 1.20.1 position/color/texture shader state, removed
 * from the legacy RenderStateShard API in Minecraft 1.21.1.
 *
 * <p>The corresponding 1.21.1 shader is exposed by
 * {@link GameRenderer#getPositionTexColorShader()}. The ShaderStateShard
 * stores the supplier and resolves the shader when the render state is applied.</p>
 */
public final class AlienEvoRenderStateCompat1201 {
    private static final RenderStateShard.ShaderStateShard POSITION_COLOR_TEX_SHADER =
            new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);

    private AlienEvoRenderStateCompat1201() {
    }

    public static RenderStateShard.ShaderStateShard positionColorTexShader() {
        return POSITION_COLOR_TEX_SHADER;
    }
}
