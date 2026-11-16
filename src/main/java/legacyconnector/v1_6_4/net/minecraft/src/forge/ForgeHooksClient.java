package legacyconnector.v1_6_4.net.minecraft.src.forge;

/** The parts of Forge 3.x's client hook class that mods call themselves: the sheet a block or item draws from. */
public class ForgeHooksClient {
    public ForgeHooksClient() {}

    /** The sheet of {@code object} (a block or item implementing {@link ITextureProvider}, or the game's block or item sheet), else {@code defaultTexture}. */
    public static String getTexture(String defaultTexture, Object object) {
        if (object instanceof ITextureProvider provider) return provider.getTextureFile();
        if (object instanceof legacyconnector.v1_7_10.net.minecraft.block.Block block) return block.getTextureFile();
        if (object instanceof legacyconnector.v1_7_10.net.minecraft.item.Item item) return item.getTextureFile();
        return defaultTexture;
    }

    /** Binds the sheet of {@code object} before it is drawn (a no-op for objects that draw from the game's sheets). */
    public static void overrideTexture(Object object) {
        String sheet = getTexture(null, object);
        if (sheet != null) MinecraftForgeClient.bindTexture(sheet);
    }
}
