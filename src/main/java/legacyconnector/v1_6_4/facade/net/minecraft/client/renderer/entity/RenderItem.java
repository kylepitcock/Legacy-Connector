package legacyconnector.v1_6_4.facade.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Facade of the 1.7.10 shim's {@code RenderItem} for 1.4.7 and 1.5.2 mods: the dropped-item stack sizes and the textured quad. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class RenderItem {
    private RenderItem() {}

    /** The number of block copies drawn for a stack on the ground (1.5.2: 1, 2 above 1, 3 above 5, 4 above 20, 5 above 40). */
    public static byte getMiniBlockCountForItemStack(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem self, ItemStack stack) {
        int size = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$get$stackSize(stack);
        if (size > 40) return 5;
        if (size > 20) return 4;
        if (size > 5) return 3;
        return (byte) (size > 1 ? 2 : 1);
    }

    /** The number of item copies drawn for a stack on the ground (1.5.2: 1, 2 above 1, 3 above 15, 4 above 31). */
    public static byte getMiniItemCountForItemStack(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem self, ItemStack stack) {
        int size = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$get$stackSize(stack);
        if (size > 31) return 4;
        if (size > 15) return 3;
        return (byte) (size > 1 ? 2 : 1);
    }

    /** {@code renderTexturedQuad(x, y, u, v, width, height)}: a quad of the bound 256 by 256 texture at the item renderer's z level. */
    public static void renderTexturedQuad(legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderItem self, int x, int y, int u, int v, int width, int height) {
        float scale = 0.00390625F;
        legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator t = legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator.mcc$get$instance();
        t.startDrawingQuads();
        t.addVertexWithUV(x, y + height, self.zLevel, u * scale, (v + height) * scale);
        t.addVertexWithUV(x + width, y + height, self.zLevel, (u + width) * scale, (v + height) * scale);
        t.addVertexWithUV(x + width, y, self.zLevel, (u + width) * scale, v * scale);
        t.addVertexWithUV(x, y, self.zLevel, u * scale, v * scale);
        t.draw();
    }
}
