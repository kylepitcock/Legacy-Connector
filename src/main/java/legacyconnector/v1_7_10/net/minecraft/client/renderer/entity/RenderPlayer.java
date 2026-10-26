package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;

/**
 * The player renderer. The modern game draws players itself; this object is what legacy render handlers receive in
 * {@code RenderPlayerEvent} so they can reach the player models ({@link #modelBipedMain} and the two armour models) and pose them.
 */
public class RenderPlayer extends RendererLivingEntity {
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/steve.png");

    public ModelBiped modelBipedMain;
    public ModelBiped modelArmorChestplate;
    public ModelBiped modelArmor;

    public RenderPlayer() {
        super(new ModelBiped(0.0F), 0.5F);
        this.modelBipedMain = (ModelBiped) this.mainModel;
        this.modelArmorChestplate = new ModelBiped(1.0F);
        this.modelArmor = new ModelBiped(0.5F);
    }

    /**
     * Draws the player's right arm in the current pose, as item renderers that hold their item in first person expect
     * (Thaumcraft's Thaumometer): the modern player renderer's own arm, with the player's skin.
     */
    public void renderFirstPersonArm(EntityPlayer player) {
        var pose = org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.pose();
        if (pose == null || player == null || !(player.mcc$entity instanceof net.minecraft.client.player.AbstractClientPlayer modern)) return;
        if (net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(modern)
                instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
            renderer.renderRightHand(pose, org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.buffers(),
                    org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext.light(), modern);
        }
    }

    @Override
    protected void renderOffsetLivingLabel(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, double x, double y, double z,
            String text, float scale, double distance) {
        super.renderOffsetLivingLabel(entity, x, y, z, text, scale, distance);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return STEVE;
    }
}
