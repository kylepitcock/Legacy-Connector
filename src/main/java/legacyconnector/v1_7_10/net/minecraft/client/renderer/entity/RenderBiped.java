package legacyconnector.v1_7_10.net.minecraft.client.renderer.entity;

import legacyconnector.v1_7_10.net.minecraft.client.model.ModelBiped;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import org.kylepitcock.legacyconnector.v1_7_10.client.RenderContext;

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
    protected ResourceLocation getEntityTexture(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity) {
        return STEVE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving l ? getEntityTexture(l) : STEVE;
    }

    @Override
    protected void preRenderCallback(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float partialTicks) {
        if (field_77071_a != 1.0F) RenderContext.scale(field_77071_a, field_77071_a, field_77071_a);
    }

    /** 1.7.10's armour texture prefixes by {@code ItemArmor.renderIndex}. */
    public static final String[] bipedArmorFilenamePrefix = {"leather", "chainmail", "iron", "diamond", "gold"};
    private static final java.util.Map<String, ResourceLocation> field_110859_k = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Forge: the texture of worn armour for a slot (0 helmet .. 3 boots) and layer type (null, or "overlay" for dyed
     * leather): the item's own {@code getArmorTexture}, else vanilla's file for its render index.
     */
    public static ResourceLocation getArmorResource(Entity entity, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, int slot, String type) {
        var item = stack == null ? null : stack.getItem();
        int index = item instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor armor ? armor.renderIndex : 2;
        if (index < 0 || index >= bipedArmorFilenamePrefix.length) index = 2;
        String path = String.format("textures/models/armor/%s_layer_%d%s.png", bipedArmorFilenamePrefix[index], slot == 2 ? 2 : 1,
                type == null ? "" : String.format("_%s", type));
        if (item != null) {
            String custom = item.getArmorTexture(stack, entity, slot, type);
            if (custom != null) path = custom;
        }
        return field_110859_k.computeIfAbsent(path, ResourceLocation::new);
    }

    /** Vanilla's armour pass order: 0 helmet, 1 chestplate, 2 leggings, 3 boots (equipment slots 4..1). */
    @Override
    protected int shouldRenderPass(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, int pass, float partialTicks) {
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack = entity.getEquipmentInSlot(4 - pass);
        if (stack == null || !(stack.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor armor)) return -1;
        if (armor.armorType != pass) return -1;
        ModelBiped model = pass == 2 ? field_82425_h : field_82423_g;
        if (model == null) return -1;
        bindTexture(getArmorResource(entity, stack, pass, null));
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

    @Override
    protected void renderEquippedItems(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity, float partialTicks) {
        if (entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving living) renderEquippedItems(living, partialTicks);
    }

    /**
     * What the biped wears on its head that is not armour (a mob head, or a block or item such as a pumpkin) and the held
     * item; mods' biped renderers override this and call it. Mob heads are drawn by the skull renderer, other head items and
     * the held item by the modern item renderer.
     */
    protected void renderEquippedItems(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity, float partialTicks) {
        mcc$renderHeadItem(entity);
        mcc$renderHeldItem(entity);
    }

    private void mcc$renderHeadItem(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack head = entity.getEquipmentInSlot(4);
        var pose = RenderContext.pose();
        if (head == null || pose == null || entity.isInvisible() || head.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor) return;
        RenderContext.push();
        try {
            modelBipedMain.bipedHead.postRender(0.0625F);
            if (head.getItem() == legacyconnector.v1_7_10.net.minecraft.init.Items.skull) {
                RenderContext.scale(1.0625F, -1.0625F, -1.0625F);
                com.mojang.authlib.GameProfile profile = null;
                var tag = head.getTagCompound();
                if (tag != null && tag.hasKey("SkullOwner", 8) && !tag.getString("SkullOwner").isEmpty()) {
                    String owner = tag.getString("SkullOwner");
                    profile = new com.mojang.authlib.GameProfile(net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(owner), owner);
                }
                legacyconnector.v1_7_10.net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer.field_147536_b
                        .func_152674_a(-0.5F, 0.0F, -0.5F, 1, 180.0F, head.getItemDamage(), profile);
            } else {
                var modern = head.mcc$modern();
                if (modern == null || modern.isEmpty()) return;
                var mc = net.minecraft.client.Minecraft.getInstance();
                // into the modern head layer's space: y up, the head's centre (as CustomHeadLayer places the HEAD transform)
                RenderContext.translate(0.0F, -0.25F, 0.0F);
                RenderContext.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                RenderContext.scale(0.625F, -0.625F, -0.625F);
                mc.getItemRenderer().renderStatic(modern, net.minecraft.world.item.ItemDisplayContext.HEAD,
                        RenderContext.light(), RenderContext.overlay(), pose, RenderContext.buffers(), (net.minecraft.world.level.Level) (Object) mc.level, 0);
            }
        } finally {
            RenderContext.pop();
        }
    }

    /** The held item is drawn by the modern item renderer in the right hand, as the modern ItemInHandLayer does. */
    private void mcc$renderHeldItem(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase entity) {
        legacyconnector.v1_7_10.net.minecraft.item.ItemStack held = entity.getHeldItem();
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
