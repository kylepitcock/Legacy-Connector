package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer;

/** Facade of the 1.7.10 shim's {@code GLAllocation} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class GLAllocation {
    private GLAllocation() {}

    /** {@code generateTextureNames()}: a fresh OpenGL texture name (1.4.7 and 1.5.2 had this instead of {@code TextureUtil.glGenTextures}). */
    public static int generateTextureNames() {
        return legacyconnector.v1_7_10.org.lwjgl.opengl.GL11Guard.glGenTextures();
    }

    /** {@code generateTextureNames(buffer)} (1.4.7): fills the buffer, from its position to its limit, with fresh OpenGL texture names. */
    public static void generateTextureNames(java.nio.IntBuffer names) {
        while (names.hasRemaining()) names.put(generateTextureNames());
    }
}
