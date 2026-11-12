package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.client.render.item.HeldItemRenderer;

/** Per-dispatch camera information consumed by original nameplate renderers. */
public class EntityRenderDispatcher {
    public static final EntityRenderDispatcher INSTANCE = new EntityRenderDispatcher();
    public LivingEntity cameraEntity;
    public legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions options=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().options;
    /** The world of the entity being drawn; renderers read it for light (original RenderManager.worldObj). */
    public legacyconnector.vb1_7_3.net.minecraft.world.World world;
    public float yaw,pitch;
    public HeldItemRenderer heldItemRenderer=new HeldItemRenderer();
    public legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager textureManager=legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager.mcc$instance();
    public void render(legacyconnector.vb1_7_3.net.minecraft.entity.Entity entity,double x,double y,double z,float yaw,float partial) {
        org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173.entity(entity,x,y,z,yaw,partial);
    }
}
