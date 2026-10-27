package legacyconnector.v1_12_2.net.minecraft.client.renderer.entity;

import legacyconnector.v1_12_2.net.minecraft.client.entity.AbstractClientPlayer;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_12_2.net.minecraft.client.model.ModelPlayer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.GlStateManager;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerArrow;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerCape;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerElytra;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerEntityOnShoulder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHandSide;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * 1.12.2 player renderer ({@code RenderManager.getSkinMap()}: "default" and "slim"). Players are drawn by the modern player renderer;
 * the layers mods add to these renderers are run on top of it ({@code LegacyPlayerLayer1122}), with this renderer's model posed as the
 * modern one. {@link #doRender} draws a player the legacy way for mods that call it themselves.
 */
public class RenderPlayer extends RenderLivingBase<AbstractClientPlayer> {
    private final boolean smallArms;
    /** How many layers vanilla added (the modern renderer draws those itself; later ones are the mods'). */
    private final int mcc$builtInLayers;

    public RenderPlayer(RenderManager renderManager) {
        this(renderManager, false);
    }

    public RenderPlayer(RenderManager renderManager, boolean useSmallArms) {
        super(renderManager, new ModelPlayer(0.0F, useSmallArms), 0.5F);
        this.smallArms = useSmallArms;
        addLayer(new LayerBipedArmor(this));
        addLayer(new LayerHeldItem(this));
        addLayer(new LayerArrow(this));
        addLayer(new LayerCape(this));
        addLayer(new LayerCustomHead(getMainModel().bipedHead));
        addLayer(new LayerElytra(this));
        addLayer(new LayerEntityOnShoulder(renderManager));
        mcc$builtInLayers = layerRenderers.size();
    }

    @Override
    public ModelPlayer getMainModel() {
        return (ModelPlayer) super.getMainModel();
    }

    /** The layers mods added (drawn over modern players). */
    public java.util.List<legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.layers.LayerRenderer<AbstractClientPlayer>> mcc$modLayers() {
        return layerRenderers.size() <= mcc$builtInLayers ? java.util.List.of() : layerRenderers.subList(mcc$builtInLayers, layerRenderers.size());
    }

    @Override
    public void doRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.isUser() && renderManager.renderViewEntity != entity) return;
        double yOffset = y;
        if (entity.isSneaking()) yOffset = y - 0.125D;
        setModelVisibilities(entity);
        GlStateManager.enableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
        super.doRender(entity, x, yOffset, z, entityYaw, partialTicks);
        GlStateManager.disableBlendProfile(GlStateManager.Profile.PLAYER_SKIN);
    }

    private void setModelVisibilities(AbstractClientPlayer clientPlayer) {
        ModelPlayer model = getMainModel();
        if (clientPlayer.isSpectator()) {
            model.setVisible(false);
            model.bipedHead.showModel = true;
            model.bipedHeadwear.showModel = true;
            return;
        }
        ItemStack main = clientPlayer.getHeldItemMainhand();
        ItemStack off = clientPlayer.getHeldItemOffhand();
        model.setVisible(true);
        model.isSneak = clientPlayer.isSneaking();
        ModelBiped.ArmPose mainPose = main.isEmpty() ? ModelBiped.ArmPose.EMPTY : ModelBiped.ArmPose.ITEM;
        ModelBiped.ArmPose offPose = off.isEmpty() ? ModelBiped.ArmPose.EMPTY : ModelBiped.ArmPose.ITEM;
        if (clientPlayer.getPrimaryHand() == EnumHandSide.RIGHT) {
            model.rightArmPose = mainPose;
            model.leftArmPose = offPose;
        } else {
            model.rightArmPose = offPose;
            model.leftArmPose = mainPose;
        }
    }

    @Override
    public ResourceLocation getEntityTexture(AbstractClientPlayer entity) {
        return entity.getLocationSkin();
    }

    @Override
    public void transformHeldFull3DItemLayer() {
        GlStateManager.translate(0.0F, 0.1875F, 0.0F);
    }

    @Override
    protected void preRenderCallback(AbstractClientPlayer entitylivingbaseIn, float partialTickTime) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected void renderEntityName(AbstractClientPlayer entityIn, double x, double y, double z, String name, double distanceSq) {
        super.renderEntityName(entityIn, x, y, z, name, distanceSq);
    }

    public void renderRightArm(AbstractClientPlayer clientPlayer) {
        renderArm(clientPlayer, getMainModel().bipedRightArm, getMainModel().bipedRightArmwear);
    }

    public void renderLeftArm(AbstractClientPlayer clientPlayer) {
        renderArm(clientPlayer, getMainModel().bipedLeftArm, getMainModel().bipedLeftArmwear);
    }

    private void renderArm(AbstractClientPlayer clientPlayer, legacyconnector.v1_12_2.net.minecraft.client.model.ModelRenderer arm,
            legacyconnector.v1_12_2.net.minecraft.client.model.ModelRenderer wear) {
        GlStateManager.color(1.0F, 1.0F, 1.0F);
        ModelPlayer model = getMainModel();
        setModelVisibilities(clientPlayer);
        bindTexture(getEntityTexture(clientPlayer));
        GlStateManager.enableBlend();
        model.swingProgress = 0.0F;
        model.isSneak = false;
        model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, clientPlayer);
        arm.rotateAngleX = 0.0F;
        arm.render(0.0625F);
        wear.rotateAngleX = 0.0F;
        wear.render(0.0625F);
        GlStateManager.disableBlend();
    }

    @Override
    protected void renderLivingAt(AbstractClientPlayer entityLivingBaseIn, double x, double y, double z) {
        super.renderLivingAt(entityLivingBaseIn, x, y, z);
    }

    @Override
    protected void applyRotations(AbstractClientPlayer entityLiving, float ageInTicks, float rotationYaw, float partialTicks) {
        if (entityLiving.isEntityAlive() && entityLiving.isPlayerSleeping()) {
            GlStateManager.rotate(mcc$bedYaw(entityLiving), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(getDeathMaxRotation(entityLiving), 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
        } else if (entityLiving.isElytraFlying()) {
            super.applyRotations(entityLiving, ageInTicks, rotationYaw, partialTicks);
            float ticks = (float) entityLiving.getTicksElytraFlying() + partialTicks;
            float tilt = Math.min(1.0F, ticks * ticks / 100.0F);
            float pitch = Entity.mcc$get$rotationPitch(entityLiving);
            GlStateManager.rotate(tilt * (-90.0F - pitch), 1.0F, 0.0F, 0.0F);
        } else {
            super.applyRotations(entityLiving, ageInTicks, rotationYaw, partialTicks);
        }
    }

    /** EntityPlayer.getBedOrientationInDegrees: the yaw of the bed the player sleeps in. */
    private static float mcc$bedYaw(AbstractClientPlayer player) {
        if (!(player.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l)) return 0.0F;
        var dir = l.getBedOrientation();
        if (dir == null) return 0.0F;
        return switch (dir) {
            case SOUTH -> 90.0F;
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            default -> 0.0F;
        };
    }
}
