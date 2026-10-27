package legacyconnector.v1_12_2.net.minecraft.client.shader;

/** 1.12.2 program linker. Modern effect instances compile and link themselves, so the helper only has to exist. */
public class ShaderLinkHelper {
    private static ShaderLinkHelper staticShaderLinkHelper;

    public static void setNewStaticShaderLinkHelper() {
        staticShaderLinkHelper = new ShaderLinkHelper();
    }

    public static ShaderLinkHelper getStaticShaderLinkHelper() {
        if (staticShaderLinkHelper == null) setNewStaticShaderLinkHelper();
        return staticShaderLinkHelper;
    }

    public void deleteShader(ShaderManager manager) {
        if (manager != null) manager.deleteShader();
    }
}
