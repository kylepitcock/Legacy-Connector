package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.render.item.HeldItemRenderer;

/** Per-dispatch camera information consumed by original nameplate renderers. */
public class EntityRenderDispatcher {
    public LivingEntity cameraEntity;
    public float yaw,pitch;
    public HeldItemRenderer heldItemRenderer=new HeldItemRenderer();
}
