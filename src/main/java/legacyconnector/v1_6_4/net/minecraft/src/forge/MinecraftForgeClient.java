package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * Forge 3.x's client hooks: texture binding by sheet path, item renderers by item id (adapted to the shim's item renderer registry, whose render
 * types are the same names plus the first-person form), and the handler registrations the layer does not act on.
 */
public class MinecraftForgeClient {
    public MinecraftForgeClient() {}

    private static final java.util.Map<Integer, IItemRenderer> RENDERERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void registerHighlightHandler(IHighlightHandler handler) {}

    public static void registerRenderContextHandler(String texture, int subid, IRenderContextHandler handler) {}

    public static void registerTextureLoadHandler(ITextureLoadHandler handler) {}

    public static void registerRenderLastHandler(IRenderWorldLastHandler handler) {}

    public static void registerSoundHandler(ISoundHandler handler) {}

    /** The sheets of a mod are cut from its jar when the mod loads, so there is nothing to upload ahead of use. */
    public static void preloadTexture(String texture) {}

    public static void bindTexture(String texture) {
        TextureManager.INSTANCE.bindTexture(texture);
    }

    public static void bindTexture(String texture, int subid) {
        TextureManager.INSTANCE.bindTexture(texture);
    }

    public static void unbindTexture() {}

    public static int getRenderPass() {
        return legacyconnector.v1_7_10.net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
    }

    public static void renderBlock(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderer,
            legacyconnector.v1_7_10.net.minecraft.block.Block block, int x, int y, int z) {
        renderer.renderStandardBlock(block, x, y, z);
    }

    public static void registerItemRenderer(int itemId, IItemRenderer renderer) {
        Item item = Ids.item(itemId);
        if (item == null || renderer == null) return;
        RENDERERS.put(itemId, renderer);
        legacyconnector.v1_7_10.net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(item,
                new legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer() {
                    @Override
                    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
                        return renderer.handleRenderType(stack, convert(type));
                    }

                    @Override
                    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
                        return renderer.shouldUseRenderHelper(convert(type), stack, IItemRenderer.ItemRendererHelper.valueOf(helper.name()));
                    }

                    @Override
                    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
                        renderer.renderItem(convert(type), stack, data);
                    }
                });
    }

    private static IItemRenderer.ItemRenderType convert(legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType type) {
        return switch (type) {
            case ENTITY -> IItemRenderer.ItemRenderType.ENTITY;
            case EQUIPPED, EQUIPPED_FIRST_PERSON -> IItemRenderer.ItemRenderType.EQUIPPED;
            case INVENTORY -> IItemRenderer.ItemRenderType.INVENTORY;
            case FIRST_PERSON_MAP -> IItemRenderer.ItemRenderType.FIRST_PERSON_MAP;
        };
    }

    public static IItemRenderer getItemRenderer(ItemStack item, IItemRenderer.ItemRenderType type) {
        if (item == null) return null;
        IItemRenderer r = RENDERERS.get(Ids.itemId(item.getItem()));
        return r != null && r.handleRenderType(item, type) ? r : null;
    }

    public static void init() {}

    public static void checkMinecraftVersion(String version, String modName) {}
}
