package mccompat.v1_7_10.net.minecraft.client.renderer.entity;

import mccompat.v1_7_10.net.minecraft.client.model.ModelBiped;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.mccompat.v1_7_10.client.RenderContext;

public class RenderBiped extends RenderLiving {
    protected ModelBiped modelBipedMain;
    protected float field_77071_a;
    protected ModelBiped field_82423_g;
    protected ModelBiped field_82425_h;
    protected ModelBiped modelArmourChestplate;

    public RenderBiped(ModelBiped model, float shadowSize) {
        this(model, shadowSize, 1.0F);
    }

    public RenderBiped(ModelBiped model, float shadowSize, float scale) {
        super(model, shadowSize);
        this.modelBipedMain = model;
        this.field_77071_a = scale;
        func_82421_b();
    }

    protected void func_82421_b() {
        field_82423_g = new ModelBiped(1.0F);
        field_82425_h = new ModelBiped(0.5F);
        modelArmourChestplate = field_82423_g;
    }

    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/player/wide/steve.png");

    /** Biped renderers (and mod subclasses) choose the texture per living entity. */
    protected ResourceLocation getEntityTexture(mccompat.v1_7_10.net.minecraft.entity.EntityLiving entity) {
        return STEVE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof mccompat.v1_7_10.net.minecraft.entity.EntityLiving l ? getEntityTexture(l) : STEVE;
    }

    @Override
    protected void preRenderCallback(mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float partialTicks) {
        if (field_77071_a != 1.0F) RenderContext.scale(field_77071_a, field_77071_a, field_77071_a);
    }

    private static final String[] ARMOR_NAMES = {"cloth", "chain", "iron", "diamond", "gold"};

    /** Vanilla's armour pass order: 0 helmet, 1 chestplate, 2 leggings, 3 boots (equipment slots 4..1). */
    @Override
    protected int shouldRenderPass(mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase entity, int pass, float partialTicks) {
        mccompat.v1_7_10.net.minecraft.item.ItemStack stack = entity.getEquipmentInSlot(4 - pass);
        if (stack == null || !(stack.getItem() instanceof mccompat.v1_7_10.net.minecraft.item.ItemArmor armor)) return -1;
        if (armor.armorType != pass) return -1;
        ModelBiped model = pass == 2 ? field_82425_h : field_82423_g;
        if (model == null) return -1;
        String custom = armor.getArmorTexture(stack, entity, pass, null);
        ResourceLocation texture;
        if (custom != null) {
            texture = new ResourceLocation(custom);
        } else {
            int index = armor.renderIndex >= 0 && armor.renderIndex < ARMOR_NAMES.length ? armor.renderIndex : 2;
            texture = new ResourceLocation("textures/models/armor/" + ARMOR_NAMES[index] + "_layer_" + (pass == 2 ? 2 : 1) + ".png");
        }
        bindTexture(texture);
        model.bipedHead.showModel = pass == 0;
        model.bipedHeadwear.showModel = pass == 0;
        model.bipedBody.showModel = pass == 1 || pass == 2;
        model.bipedRightArm.showModel = pass == 1;
        model.bipedLeftArm.showModel = pass == 1;
        model.bipedRightLeg.showModel = pass == 2 || pass == 3;
        model.bipedLeftLeg.showModel = pass == 2 || pass == 3;
        model.heldItemRight = modelBipedMain.heldItemRight;
        model.heldItemLeft = modelBipedMain.heldItemLeft;
        model.isSneak = modelBipedMain.isSneak;
        model.aimedBow = modelBipedMain.aimedBow;
        setRenderPassModel(model);
        return 1;
    }

    /** The held item is drawn by the modern item renderer in the right hand, as the modern ItemInHandLayer does. */
    @Override
    protected void renderEquippedItems(mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float partialTicks) {
        mccompat.v1_7_10.net.minecraft.item.ItemStack held = entity.getHeldItem();
        var pose = RenderContext.pose();
        if (held == null || pose == null || entity.isInvisible()) return;
        var modern = held.mcc$modern();
        if (modern == null || modern.isEmpty()) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        RenderContext.push();
        try {
            modelBipedMain.bipedRightArm.postRender(0.0625F);
            RenderContext.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            RenderContext.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            RenderContext.translate(1.0F / 16.0F, 0.125F, -0.625F);
            mc.getItemRenderer().renderStatic(modern, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    RenderContext.light(), RenderContext.overlay(), pose, RenderContext.buffers(), (net.minecraft.world.level.Level) (Object) mc.level, 0);
        } finally {
            RenderContext.pop();
        }
    }
}
