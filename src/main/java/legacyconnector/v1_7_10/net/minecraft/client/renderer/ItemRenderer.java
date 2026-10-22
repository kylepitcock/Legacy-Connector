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

    public void renderItem(EntityLivingBase entity, ItemStack stack, int pass, legacyconnector.v1_7_10.net.minecraftforge.client.IItemRenderer.ItemRenderType type) {
        renderItem(entity, stack, pass);
    }
}
