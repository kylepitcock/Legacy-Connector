package legacyconnector.v1_6_4.facade.net.minecraftforge.client;

/** Facade of the 1.7.10 shim's {@code ForgeHooksClient} for 1.4.7 and 1.5.2 mods. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class ForgeHooksClient {
    private ForgeHooksClient() {}

    /**
     * Forge 1.4.7 {@code beforeBlockRender(block, renderer)}: bound the texture file the block named ({@code getTextureFile()}) before a custom renderer drew it.
     * Block sprites are cut out of those files onto the one block atlas ({@code SpriteSheets147}), which is what every block draw binds, so there is no file to bind.
     */
    public static void beforeBlockRender(legacyconnector.v1_7_10.net.minecraft.block.Block block,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer) {
    }

    /** Forge 1.4.7 {@code afterBlockRender(block, renderer)}: bound the default terrain file again; see {@link #beforeBlockRender}. */
    public static void afterBlockRender(legacyconnector.v1_7_10.net.minecraft.block.Block block,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer) {
    }

    /** Forge 1.4.7 {@code getArmorTexture(armor, default)}: the item's own armor texture path when it names one (a {@code getArmorTextureFile} provider), else the default. */
    public static String getArmorTexture(legacyconnector.v1_7_10.net.minecraft.item.ItemStack armor, String defaultTexture) {
        if (armor != null && armor.getItem() instanceof legacyconnector.v1_6_4.net.minecraft.src.forge.IArmorTextureProvider provider) {
            String own = provider.getArmorTextureFile(armor);
            if (own != null) return own;
        }
        return defaultTexture;
    }
}
