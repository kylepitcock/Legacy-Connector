package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * Renderer of thrown items (snowballs, eggs, mod grenades): draws the item's icon as a billboard. Constructed by mods when they
 * register their projectile renderers. The item is drawn through the modern item renderer, turned to face the camera.
 */
public class RenderSnowball extends Render {
    private final Item field_94151_a;
    private final int field_94150_f;

    public RenderSnowball(Item item) {
        this(item, 0);
    }

    public RenderSnowball(Item item, int damage) {
        this.field_94151_a = item;
        this.field_94150_f = damage;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        var pose = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.pose();
        if (pose == null || field_94151_a == null) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack = new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(field_94151_a, 1, field_94150_f);
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(mc.gameRenderer.getMainCamera().rotation());
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        pose.scale(2.0F, 2.0F, 2.0F);
        mc.getItemRenderer().renderStatic(stack.mcc$modern(), net.minecraft.world.item.ItemDisplayContext.GROUND,
                org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.light(), org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.overlay(), pose,
                org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.buffers(), (net.minecraft.world.level.Level) (Object) mc.level, 0);
        pose.popPose();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
