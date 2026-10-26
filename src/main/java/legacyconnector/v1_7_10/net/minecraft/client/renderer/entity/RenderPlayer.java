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

    /**
     * The armour passes (0 helmet .. 3 boots): binds the worn piece's texture and shows the matching parts of the armour
     * model (leggings use the thinner one). 15 for an enchanted piece (1.7.10 drew the glint then), 1 otherwise, -1 for none.
     */
    protected int shouldRenderPass(legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer player, int pass, float partialTicks) {
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack = player.inventory.armorItemInSlot(3 - pass);
        if (stack == null || !(stack.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor)) return -1;
        bindTexture(RenderBiped.getArmorResource(player, stack, pass, null));
        ModelBiped model = pass == 2 ? modelArmor : modelArmorChestplate;
        model.bipedHead.showModel = pass == 0;
        model.bipedHeadwear.showModel = pass == 0;
        model.bipedBody.showModel = pass == 1 || pass == 2;
        model.bipedRightArm.showModel = pass == 1;
        model.bipedLeftArm.showModel = pass == 1;
        model.bipedRightLeg.showModel = pass == 2 || pass == 3;
        model.bipedLeftLeg.showModel = pass == 2 || pass == 3;
        model.onGround = mainModel.onGround;
        model.isRiding = mainModel.isRiding;
        model.isChild = mainModel.isChild;
        model.heldItemRight = modelBipedMain.heldItemRight;
        model.heldItemLeft = modelBipedMain.heldItemLeft;
        model.isSneak = modelBipedMain.isSneak;
        model.aimedBow = modelBipedMain.aimedBow;
        setRenderPassModel(model);
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor3f(1.0F, 1.0F, 1.0F);
        return stack.isItemEnchanted() ? 15 : 1;
    }

    @Override
    protected int shouldRenderPass(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, int pass, float partialTicks) {
        return entity instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer p ? shouldRenderPass(p, pass, partialTicks) : -1;
    }

    /** The second layer of an armour pass: the undyed overlay of dyed (leather) armour. */
    protected void func_82408_c(legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer player, int pass, float partialTicks) {
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack = player.inventory.armorItemInSlot(3 - pass);
        if (stack != null && stack.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor) {
            bindTexture(RenderBiped.getArmorResource(player, stack, pass, "overlay"));
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glColor3f(1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    protected void func_82408_c(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, int pass, float partialTicks) {
        if (entity instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer p) func_82408_c(p, pass, partialTicks);
    }

    /** Players are drawn at 15/16 of the model size. */
    protected void preRenderCallback(legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer player, float partialTicks) {
        legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glScalef(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected void preRenderCallback(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float partialTicks) {
        if (entity instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer p) preRenderCallback(p, partialTicks);
    }

    /** A sleeping player lies along its bed; otherwise the living rotation (body yaw, death roll). */
    protected void rotateCorpse(legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer player, float age, float bodyYaw, float partialTicks) {
        var modern = player.mcc$entity instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
        var bed = modern == null ? null : modern.getBedOrientation();
        if (player.isEntityAlive() && player.isPlayerSleeping() && bed != null) {
            float degrees = switch (bed) {
                case SOUTH -> 90.0F;
                case NORTH -> 270.0F;
                case EAST -> 180.0F;
                default -> 0.0F;
            };
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glRotatef(degrees, 0.0F, 1.0F, 0.0F);
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glRotatef(getDeathMaxRotation(player), 0.0F, 0.0F, 1.0F);
            legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glRotatef(270.0F, 0.0F, 1.0F, 0.0F);
        } else {
            super.rotateCorpse(player, age, bodyYaw, partialTicks);
        }
    }

    @Override
    protected void rotateCorpse(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float age, float bodyYaw, float partialTicks) {
        if (entity instanceof legacyconnector.v1_7_10.net.minecraft.client.entity.AbstractClientPlayer p) rotateCorpse(p, age, bodyYaw, partialTicks);
        else super.rotateCorpse(entity, age, bodyYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return STEVE;
    }
}
