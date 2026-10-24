package legacyconnector.v1_7_10.net.minecraftforge.client;

/**
 * Forge client hooks. {@code worldRenderPass} is the world pass being meshed (0 cut out, 1 translucent): block renderers
 * that draw in both passes (BuildCraft pipes) read it to decide what to emit. -1 outside chunk meshing.
 */
public class ForgeHooksClient {
    public static int worldRenderPass = -1;

    /** The item/entity render pass legacy renderers query through {@code MinecraftForgeClient.getRenderPass()}. */
    public static int renderPass = -1;

    public static void setRenderPass(int pass) {
        renderPass = pass;
    }

    public static int getWorldRenderPass() {
        return worldRenderPass;
    }

    /** Draws the stack as a GUI icon through the item's modern model; true when drawn. */
    public static boolean renderInventoryItem(legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks blocks,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures,
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, boolean inColor, float zLevel, float x, float y) {
        if (stack == null || stack.getItem() == null) return false;
        legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem.getInstance().renderItemIntoGUI(null, textures, stack, (int) x, (int) y);
        return true;
    }

    /** Custom item-entity rendering is not available; false makes the caller draw the item normally. */
    public static boolean renderEntityItem(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack,
            float bobing, float rotation, java.util.Random random, legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks blocks, int count) {
        return false;
    }
}
