package legacyconnector.v1_12_2.net.minecraft.client.shader;

/** 1.12.2's stand-in uniform (what getShaderUniformOrDefault returned for a missing name): takes values and ignores them. */
public class ShaderDefault extends ShaderUniform {
    public ShaderDefault() {
        super("dummy", 4, 1, null);
    }
}
