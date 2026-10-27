package legacyconnector.v1_12_2.net.minecraft.client.shader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import legacyconnector.v1_12_2.net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.renderer.EffectInstance;

/**
 * 1.12.2 shader program ({@code shaders/program/<name>.json} with its vertex and fragment shaders), run as a modern
 * {@link EffectInstance}. A program the modern game cannot compile (GLSL the core profile rejects) logs once and draws nothing.
 */
public class ShaderManager {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/Shaders");
    private static final ShaderDefault DEFAULT_SHADER_UNIFORM = new ShaderDefault();
    private final String programFilename;
    private final EffectInstance mcc$effect;
    private final Map<String, ShaderUniform> mcc$uniforms = new HashMap<>();

    public ShaderManager(IResourceManager resourceManager, String programName) {
        this.programFilename = programName;
        EffectInstance effect = null;
        if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            try {
                effect = new EffectInstance(net.minecraft.client.Minecraft.getInstance().getResourceManager(), programName);
            } catch (Exception | LinkageError e) {
                LOG.warn("The shader program {} could not be loaded: {}", programName, e.toString());
            }
        } else {
            LOG.warn("The shader program {} was made off the render thread and is not loaded", programName);
        }
        this.mcc$effect = effect;
    }

    ShaderManager(EffectInstance effect) {
        this.programFilename = effect == null ? "" : effect.getName();
        this.mcc$effect = effect;
    }

    public void deleteShader() {
        if (mcc$effect != null) mcc$effect.close();
    }

    public void endShader() {
        if (mcc$effect != null) mcc$effect.clear();
    }

    public void useShader() {
        if (mcc$effect != null) mcc$effect.apply();
    }

    /** A texture for a sampler: a texture id (Integer), a Framebuffer (its colour texture) or a texture object. */
    public void addSamplerTexture(String samplerName, Object samplerTexture) {
        if (mcc$effect == null || samplerTexture == null) return;
        IntSupplier id;
        if (samplerTexture instanceof Integer i) id = () -> i;
        else if (samplerTexture instanceof Framebuffer fb) id = () -> fb.mcc$target() == null ? fb.framebufferTexture : fb.mcc$target().getColorTextureId();
        else if (samplerTexture instanceof legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.ITextureObject t) id = t::getGlTextureId;
        else return;
        mcc$effect.setSampler(samplerName, id);
    }

    public ShaderUniform getShaderUniform(String name) {
        if (mcc$effect == null) return null;
        ShaderUniform u = mcc$uniforms.get(name);
        if (u == null) {
            var modern = mcc$effect.getUniform(name);
            if (modern == null) return null;
            u = new ShaderUniform(name, 4, 1, this, modern);
            mcc$uniforms.put(name, u);
        }
        return u;
    }

    public ShaderUniform getShaderUniformOrDefault(String name) {
        ShaderUniform u = getShaderUniform(name);
        return u == null ? DEFAULT_SHADER_UNIFORM : u;
    }

    public int getProgram() {
        return mcc$effect == null ? 0 : mcc$effect.getId();
    }

    public String getProgramFilename() {
        return programFilename;
    }
}
