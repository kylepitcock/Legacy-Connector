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

    /** The item's own armor texture for the slot and layer type, else the default. */
    public static String getArmorTexture(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack armor,
            String defaultTexture, int slot, String type) {
        if (armor == null || armor.getItem() == null) return defaultTexture;
        String result = armor.getItem().getArmorTexture(armor, entity, slot, type);
        return result != null ? result : defaultTexture;
    }

    /** The item's own armor model for the slot, else the default. */
    public static legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped getArmorModel(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity,
            legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int slot, legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped defaultModel) {
        if (stack == null || stack.getItem() == null) return defaultModel;
        var model = stack.getItem().getArmorModel(entity, stack, slot);
        return model == null ? defaultModel : model;
    }

    /**
     * Draws a held item with its custom renderer, placed in the hand as 1.7.10 did: a renderer using the equipped-block
     * helper draws centred on the origin, others get the flat item's hold transform.
     */
    public static void renderEquippedItem(legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType type,
            legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer customRenderer, legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks renderBlocks,
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack item) {
        if (customRenderer == null) return;
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glPushMatrix();
        try {
            if (customRenderer.shouldUseRenderHelper(type, item, legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRendererHelper.EQUIPPED_BLOCK)) {
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            } else {
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glTranslatef(0.0F, -0.3F, 0.0F);
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glScalef(1.5F, 1.5F, 1.5F);
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glRotatef(335.0F, 0.0F, 0.0F, 1.0F);
                legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glTranslatef(-0.9375F, -0.0625F, 0.0F);
            }
            customRenderer.renderItem(type, item, renderBlocks, entity);
        } finally {
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glPopMatrix();
        }
    }

    private static int skyX;
    private static int skyZ;
    private static boolean skyInit;
    private static int skyRGBMultiplier;

    /**
     * The sky colour at the player, blended over the biomes around (as far as the modern biome blend setting reaches), as
     * 1.7.10 computed it for the sky renderer; cached while the player stays in the same column.
     */
    public static int getSkyBlendColour(legacyconnector.v1_7_10.net.minecraft.world.World world, int playerX, int playerY, int playerZ) {
        if (skyInit && playerX == skyX && playerZ == skyZ) return skyRGBMultiplier;
        var level = world == null ? null : world.mcc$fullLevel();
        if (level == null) return 0x78A7FF;
        int distance = 0;
        try {
            distance = net.minecraft.client.Minecraft.getInstance().options.biomeBlendRadius().get();
        } catch (RuntimeException | LinkageError e) {
            // no client options: no blending
        }
        int r = 0, g = 0, b = 0, count = 0;
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = -distance; x <= distance; ++x) {
            for (int z = -distance; z <= distance; ++z) {
                pos.set(playerX + x, playerY, playerZ + z);
                int colour = level.getBiome(pos).value().getSkyColor();
                r += (colour & 0xFF0000) >> 16;
                g += (colour & 0x00FF00) >> 8;
                b += colour & 0x0000FF;
                count++;
            }
        }
        skyX = playerX;
        skyZ = playerZ;
        skyInit = true;
        skyRGBMultiplier = (r / count & 255) << 16 | (g / count & 255) << 8 | b / count & 255;
        return skyRGBMultiplier;
    }

    /** Custom item-entity rendering is not available; false makes the caller draw the item normally. */
    public static boolean renderEntityItem(legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack,
            float bobing, float rotation, java.util.Random random, legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager textures,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks blocks, int count) {
        return false;
    }
}
