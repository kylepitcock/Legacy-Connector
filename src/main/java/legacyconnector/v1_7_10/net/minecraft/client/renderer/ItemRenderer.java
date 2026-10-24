package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

/** 1.7.10's first-person/held item renderer: mods' entity renderers call it to draw the item an entity holds. */
public class ItemRenderer {
    public void renderItem(EntityLivingBase entity, ItemStack stack, int pass) {
        var pose = RenderContext.pose();
        var modern = ItemStack.mcc$toModern(stack);
        if (pose == null || modern == null || modern.isEmpty()) return;
        var mc = Minecraft.getInstance();
        LivingEntity living = entity != null && entity.mcc$entity instanceof LivingEntity l ? l : null;
        mc.getItemRenderer().renderStatic(living, modern, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, RenderContext.buffers(), mc.level,
                RenderContext.light(), RenderContext.overlay(), 0);
    }

    /**
     * Draws an item icon as a flat sprite with thickness: the front and back faces, and one strip per pixel along each edge
     * (the thick look of held tools). {@code u1..v2} are the icon's atlas coordinates, {@code width}/{@code height} its pixels.
     */
    public static void renderItemIn2D(Tessellator t, float u1, float v1, float u2, float v2, int width, int height, float thickness) {
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(0.0D, 0.0D, 0.0D, u1, v2);
        t.addVertexWithUV(1.0D, 0.0D, 0.0D, u2, v2);
        t.addVertexWithUV(1.0D, 1.0D, 0.0D, u2, v1);
        t.addVertexWithUV(0.0D, 1.0D, 0.0D, u1, v1);
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, -1.0F);
        t.addVertexWithUV(0.0D, 1.0D, -thickness, u1, v1);
        t.addVertexWithUV(1.0D, 1.0D, -thickness, u2, v1);
        t.addVertexWithUV(1.0D, 0.0D, -thickness, u2, v2);
        t.addVertexWithUV(0.0D, 0.0D, -thickness, u1, v2);
        t.draw();
        float halfU = 0.5F * (u1 - u2) / width;
        float halfV = 0.5F * (v2 - v1) / height;
        t.startDrawingQuads();
        t.setNormal(-1.0F, 0.0F, 0.0F);
        for (int k = 0; k < width; ++k) {
            float x = (float) k / width;
            float u = u1 + (u2 - u1) * x - halfU;
            t.addVertexWithUV(x, 0.0D, -thickness, u, v2);
            t.addVertexWithUV(x, 0.0D, 0.0D, u, v2);
            t.addVertexWithUV(x, 1.0D, 0.0D, u, v1);
            t.addVertexWithUV(x, 1.0D, -thickness, u, v1);
        }
        t.draw();
        t.startDrawingQuads();
        t.setNormal(1.0F, 0.0F, 0.0F);
        for (int k = 0; k < width; ++k) {
            float x = (float) k / width;
            float u = u1 + (u2 - u1) * x - halfU;
            float x2 = x + 1.0F / width;
            t.addVertexWithUV(x2, 1.0D, -thickness, u, v1);
            t.addVertexWithUV(x2, 1.0D, 0.0D, u, v1);
            t.addVertexWithUV(x2, 0.0D, 0.0D, u, v2);
            t.addVertexWithUV(x2, 0.0D, -thickness, u, v2);
        }
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 1.0F, 0.0F);
        for (int k = 0; k < height; ++k) {
            float y = (float) k / height;
            float v = v2 + (v1 - v2) * y - halfV;
            float y2 = y + 1.0F / height;
            t.addVertexWithUV(0.0D, y2, 0.0D, u1, v);
            t.addVertexWithUV(1.0D, y2, 0.0D, u2, v);
            t.addVertexWithUV(1.0D, y2, -thickness, u2, v);
            t.addVertexWithUV(0.0D, y2, -thickness, u1, v);
        }
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, -1.0F, 0.0F);
        for (int k = 0; k < height; ++k) {
            float y = (float) k / height;
            float v = v2 + (v1 - v2) * y - halfV;
            t.addVertexWithUV(1.0D, y, 0.0D, u2, v);
            t.addVertexWithUV(0.0D, y, 0.0D, u1, v);
            t.addVertexWithUV(0.0D, y, -thickness, u1, v);
            t.addVertexWithUV(1.0D, y, -thickness, u2, v);
        }
        t.draw();
    }

    public void renderItem(EntityLivingBase entity, ItemStack stack, int pass, legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType type) {
        renderItem(entity, stack, pass);
    }
}
