package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.RenderItem;
import legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityPotion;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Vanilla1122;

/** 1.12.2 thrown potion renderer: the thrown potion's own stack (splash or lingering, with its colour) facing the camera. */
public class RenderPotion extends RenderSnowball<EntityPotion> {
    public RenderPotion(RenderManager renderManagerIn, RenderItem itemRendererIn) {
        super(renderManagerIn, Vanilla1122.item("POTIONITEM"), itemRendererIn);
    }

    @Override
    public ItemStack getStackToRender(EntityPotion entityIn) {
        return entityIn.getPotion();
    }
}
